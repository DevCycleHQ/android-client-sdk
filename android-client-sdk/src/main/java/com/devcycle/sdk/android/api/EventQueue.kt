package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCRequestException
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.model.Event
import com.devcycle.sdk.android.model.UserAndEvents
import com.devcycle.sdk.android.util.DevCycleLogger
import com.devcycle.sdk.android.util.Scheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

internal class EventQueue constructor(
    private val request: Request,
    private val getUser: () -> PopulatedUser,
    private val coroutineScope: CoroutineScope,
    flushInMs: Long
) {
    private val eventQueue: MutableList<Event> = mutableListOf()
    private val eventPayloadsToFlush: MutableList<UserAndEvents> = mutableListOf()
    internal val aggregateEventMap: HashMap<String, HashMap<String, Event>> = HashMap()

    private val scheduler = Scheduler(coroutineScope, flushInMs)
    private var scheduleJob: Job? = null
    // mutex to control flushing events, ensuring only one operation at a time
    private val flushMutex = Mutex()
    // mutex to gate modifications to the aggregateEventMap
    private val aggregateMutex = Mutex()
    // mutex to gate modifications to the eventQueue
    private val queueMutex = Mutex()
    // ensures flushEvents does not get called after the sdk is closed
    val isClosed = AtomicBoolean(false)
    private val flushAgain = AtomicBoolean(true)
    private var closeCallback: DevCycleCallback<String>? = null
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
                    DevCycleLogger.d("No events to flush.")
                    return@withLock
                }

                DevCycleLogger.i("DevCycle Flush " + eventsToFlush.size + " Events")

                val payload = UserAndEvents(user.copy(), eventsToFlush)

                eventPayloadsToFlush.add(payload)

                var firstError: Throwable? = null

                val jobs = flow {
                    eventPayloadsToFlush.map {
                        try {
                            request.publishEvents(it)
                            emit(it)
                            DevCycleLogger.i("DevCycle Flushed " + payload.events.size + " Events.")
                        } catch (t: DVCRequestException) {
                            if (t.isRetryable) {
                                DevCycleLogger.e(t, "Error with event flushing, will be retried")
                                // Don't raise the error but keep the payload in the queue, it will be
                                // retried on the next flush
                                firstError = firstError ?: t
                            } else {
                                DevCycleLogger.e(t, "Non-retryable error with event flushing.")
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
                DevCycleLogger.e(t, "Error flushing events")
                result = DVCFlushResult(false, t)
            }

            if (isClosed.get()) {
                // The DevCycleClient has been closed and the queue is empty, callback with success
                if (eventQueue.size == 0 ) {
                    closeCallback?.onSuccess("Event queue is clear")
                    // The DevCycleClient has been closed and something went wrong flushing
                } else if (result.exception != null) {
                    closeCallback?.onError(result.exception as Throwable)
                    // The DevCycleClient has been closed, but more events were added to the queue
                    // while the queue was flushing (but before it was closed)
                } else if (flushAgain.get()) {
                    flushAgain.set(false)
                    flushEvents()
                } else {
                    closeCallback?.onError(Throwable("Error trying to flush events after closing DevCycle"))
                }
            }
        }

        if ((eventQueue.size > 0 || !result.success) && !isClosed.get()) {
            scheduleJob = scheduler.scheduleWithDelay { run() }
        }


        return result
    }

    /**
     * Queue Event for producing
     */
    fun queueEvent(event: Event) {
        if (isClosed.get()) {
            DevCycleLogger.w("Attempting to queue event after closing DevCycle.")
            return
        }
        runBlocking {
            queueMutex.withLock {
                eventQueue.add(event)
                DevCycleLogger.i("Event queued successfully %s", event)
                scheduleJob = scheduler.scheduleWithDelay { run() }
            }
        }
    }

    /**
     * Queue DVCEvent that can be aggregated together, where multiple calls are aggregated
     * by incrementing the 'value' field.
     */
    @Throws(IllegalArgumentException::class)
    fun queueAggregateEvent(event: Event) {
        if (isClosed.get()) {
            DevCycleLogger.w("Attempting to queue aggregate event after closing DVC.")
            return
        }
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

                scheduleJob = scheduler.scheduleWithDelay { run() }
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
            DevCycleLogger.i("Skipping event flush due to pending flush operation")
            return
        }
        coroutineScope.launch {
            flushEvents()
        }
    }

    suspend fun close(callback: DevCycleCallback<String>?) {
        isClosed.set(true)
        closeCallback = callback
        flushEvents()
        scheduleJob?.cancelAndJoin()
    }
}