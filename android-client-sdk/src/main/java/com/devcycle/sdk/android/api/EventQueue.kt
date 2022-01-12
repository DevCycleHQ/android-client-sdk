package com.devcycle.sdk.android.api

import android.util.Log
import com.devcycle.sdk.android.exception.DVCRequestException
import com.devcycle.sdk.android.model.Event
import com.devcycle.sdk.android.model.User
import com.devcycle.sdk.android.model.UserAndEvents
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.*

internal class EventQueue constructor(
    private val request: Request,
    private val getUser: () -> User,
    private val coroutineScope: CoroutineScope
) : TimerTask(){
    private val TAG = EventQueue::class.java.simpleName

    private val eventQueue: MutableList<Event> = mutableListOf()
    private val eventPayloadsToFlush: MutableList<UserAndEvents> = mutableListOf()
    private val aggregateEventMap: HashMap<String, HashMap<String, Event>> = HashMap()

    // mutex to control flushing events, ensuring only one operation at a time
    private val flushMutex = Mutex()
    // mutex to gate modifications to the aggregateEventMap
    private val aggregateMutex = Mutex()
    // mutex to gate modifications to the eventQueue
    private val queueMutex = Mutex()

    suspend fun flushEvents(throwOnFailure: Boolean = false) {
        flushMutex.withLock {
            val user = getUser()
            val currentEventQueue = eventQueue.toMutableList()
            val eventsToFlush: MutableList<Event> = mutableListOf()
            eventsToFlush.addAll(currentEventQueue)

            queueMutex.withLock {
                eventQueue.removeAll(currentEventQueue)
            }

            aggregateMutex.withLock {
                eventsToFlush.addAll(eventsFromAggregateEventMap())
                aggregateEventMap.clear()
            }

            if (eventsToFlush.size == 0) {
                Log.i(TAG, "No events to flush.")
                return
            }

            Log.i(TAG, "DVC Flush ${eventsToFlush.size} Events")

            val payload = UserAndEvents(user.copy(), eventsToFlush)

            eventPayloadsToFlush.add(payload)

            var firstError: Throwable? = null

            val jobs = flow {
                eventPayloadsToFlush.map {
                    try {
                        request.publishEvents(it)
                        emit(it)
                        Log.i(TAG, "DVC Flushed ${payload.events.size} Events.")
                    } catch (t: DVCRequestException) {
                        if (t.isRetryable) {
                            Log.e(TAG, "Error with event flushing, will be retried", t)
                            // Don't raise the error but keep the payload in the queue, it will be
                            // retried on the next flush
                            firstError = firstError ?: t
                        } else {
                            Log.e(TAG, "Non-retryable error with event flushing.", t)
                            emit(it)
                        }
                    }
                }
            }

            val successful = jobs.toList()

            eventPayloadsToFlush.removeAll(successful)

            if (eventPayloadsToFlush.size > 0 && throwOnFailure) {
                throw Throwable("Failed to completely flush events queue.", firstError)
            }
        }
    }

    /**
     * Queue Event for producing
     */
    fun queueEvent(event: Event) {
        runBlocking {
            queueMutex.withLock {
                eventQueue.add(event)
            }
        }
    }

    /**
     * Queue DVCEvent that can be aggregated together, where multiple calls are aggregated
     * by incrementing the 'value' field.
     */
    @Throws(IllegalArgumentException::class)
    fun queueAggregateEvent(event: Event) {
        runBlocking {
            aggregateMutex.withLock {
                if (event.target == null || event.target == "") {
                    throw IllegalArgumentException("Target must be set")
                }
                if (event.type == "") {
                    throw IllegalArgumentException("Type must be set")
                }

                val aggEventType = aggregateEventMap[event.type]
                when {
                    aggEventType == null -> {
                        val map = aggregateEventMap.getOrPut(event.type, { HashMap<String, Event>() })
                        map[event.target] = event
                    }
                    aggEventType.containsKey(event.target) -> {
                        aggEventType[event.target] = event.copy(
                            value = aggEventType[event.target]?.value?.plus(BigDecimal.ONE)
                        )
                    }
                    else -> {
                        aggEventType[event.target] = event
                    }
                }
            }
        }
    }

    private fun eventsFromAggregateEventMap(): List<Event> {
        val eventList = mutableListOf<Event>()
        aggregateEventMap.flatMap { it.value.values }.forEach { eventList += it }
        return eventList
    }

    override fun run() {
        if (flushMutex.isLocked) {
            Log.i(TAG, "Skipping event flush due to pending flush operation")
            return
        }
        coroutineScope.launch {
            try {
                flushEvents()
            } catch (t: Throwable) {
                Log.e(TAG, "Error flushing events in background", t)
            }
        }
    }
}