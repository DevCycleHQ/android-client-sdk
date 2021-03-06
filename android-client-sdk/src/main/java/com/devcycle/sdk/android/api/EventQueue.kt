package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCRequestException
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.model.Event
import com.devcycle.sdk.android.model.UserAndEvents
import com.devcycle.sdk.android.util.Scheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.math.BigDecimal
import java.util.*

internal class EventQueue constructor(
    private val request: Request,
    private val getUser: () -> User,
    private val coroutineScope: CoroutineScope,
    flushInMs: Long
) {
    private val eventQueue: MutableList<Event> = mutableListOf()
    private val eventPayloadsToFlush: MutableList<UserAndEvents> = mutableListOf()
    internal val aggregateEventMap: HashMap<String, HashMap<String, Event>> = HashMap()

    private val scheduler = Scheduler(coroutineScope, flushInMs)

    // mutex to control flushing events, ensuring only one operation at a time
    private val flushMutex = Mutex()
    // mutex to gate modifications to the aggregateEventMap
    private val aggregateMutex = Mutex()
    // mutex to gate modifications to the eventQueue
    private val queueMutex = Mutex()

    suspend fun flushEvents(): DVCFlushResult {
        var result = DVCFlushResult(false)
        flushMutex.withLock {
            try {
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
                    Timber.i("No events to flush.")
                    return@withLock
                }

                Timber.i("DVC Flush " + eventsToFlush.size + " Events")

                val payload = UserAndEvents(user.copy(), eventsToFlush)

                eventPayloadsToFlush.add(payload)

                var firstError: Throwable? = null

                val jobs = flow {
                    eventPayloadsToFlush.map {
                        try {
                            request.publishEvents(it)
                            emit(it)
                            Timber.i("DVC Flushed " + payload.events.size + " Events.")
                        } catch (t: DVCRequestException) {
                            if (t.isRetryable) {
                                Timber.e(t, "Error with event flushing, will be retried")
                                // Don't raise the error but keep the payload in the queue, it will be
                                // retried on the next flush
                                firstError = firstError ?: t
                            } else {
                                Timber.e(t, "Non-retryable error with event flushing.")
                                emit(it)
                            }
                        }
                    }
                }

                val successful = jobs.toList()

                eventPayloadsToFlush.removeAll(successful)

                result = if (eventPayloadsToFlush.size > 0) {
                    DVCFlushResult(false, Throwable("Failed to completely flush events queue", firstError))
                } else {
                    DVCFlushResult(true)
                }
            } catch(t: Throwable) {
                Timber.e(t, "Error flushing events")
                result = DVCFlushResult(false, t)
            }
        }

        if (eventQueue.size > 0 || !result.success) {
            scheduler.scheduleWithDelay { run() }
        }

        return result
    }

    /**
     * Queue Event for producing
     */
    fun queueEvent(event: Event) {
        runBlocking {
            queueMutex.withLock {
                eventQueue.add(event)
                Timber.i("Event queued successfully %s", event)
                scheduler.scheduleWithDelay { run() }
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

                var aggEventType = aggregateEventMap[event.type]

                if (aggEventType == null) {
                    aggEventType = aggregateEventMap.getOrPut(event.type) { HashMap<String, Event>() }
                    aggEventType[event.target] = event
                } else if (aggEventType.containsKey(event.target)) {
                    aggEventType[event.target] = event.copy(
                        value = aggEventType[event.target]?.value?.plus(BigDecimal.ONE)
                    )
                } else {
                    aggEventType[event.target] = event
                }

                scheduler.scheduleWithDelay { run() }
            }
        }
    }

    private fun eventsFromAggregateEventMap(): List<Event> {
        val eventList = mutableListOf<Event>()
        aggregateEventMap.flatMap { it.value.values }.forEach { eventList += it }
        return eventList
    }

    private fun run() {
        if (flushMutex.isLocked) {
            Timber.i("Skipping event flush due to pending flush operation")
            return
        }
        coroutineScope.launch {
            flushEvents()
        }
    }
}