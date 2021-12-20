package com.devcycle.sdk.android.api

import android.util.Log
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.model.DVCRequestEvent
import com.devcycle.sdk.android.model.Event
import com.devcycle.sdk.android.model.User
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashMap

internal class EventQueue constructor(
    private val request: Request,
    private var user: User,
    private val eventQueue: MutableList<DVCEvent> = mutableListOf(),
    private val eventsToFlush: MutableList<Event> = mutableListOf(),
    private val aggregateEventMap: HashMap<String, HashMap<String, DVCRequestEvent>> = hashMapOf(),
) : TimerTask(){
    private val TAG = EventQueue::class.java.simpleName
    private var config: BucketedUserConfig? = null

    fun initialize(config: BucketedUserConfig) {
        this.config = config
    }

    @Synchronized
    fun flushEvents(callback: DVCCallback<DVCResponse?>? = null) {
        if (config == null) {
            Log.e(TAG, "DVCClient not initialized to flush events!")
            return
        }
        val currentEventQueue = eventQueue.toMutableList()
        eventsToFlush.addAll(eventsFromDvcEvent(currentEventQueue))
        eventsToFlush.addAll(eventsFromAggregateEventMap())

        if (eventsToFlush.size == 0) {
            Log.i(TAG, "No events to flush.")
            return
        }

        Log.i(TAG, "DVC Flush ${eventsToFlush.size} Events")

        eventQueue.clear()
        aggregateEventMap.clear()

        request.publishEvents(user, eventsToFlush, object : DVCCallback<DVCResponse?> {
            override fun onSuccess(result: DVCResponse?) {
                Log.i(TAG, "DVC Flushed ${eventsToFlush.size} Events.")
                eventsToFlush.clear()
                callback?.onSuccess(result)
            }

            override fun onError(t: Throwable) {
                callback?.onError(t)
            }
        })
    }

    /**
     * Queue Event for producing
     */
    fun queueEvent(event: DVCEvent) {
        eventQueue.add(event)
    }

    /**
     * Queue DVCEvent that can be aggregated together, where multiple calls are aggregated
     * by incrementing the 'value' field.
     */
    @Throws(IllegalArgumentException::class)
    fun queueAggregateEvent(event: DVCRequestEvent) {
        if (event.target == null || event.target == "") {
            throw IllegalArgumentException("Target must be set")
        }
        if (event.type == "") {
            throw IllegalArgumentException("Type must be set")
        }
        event.date = Calendar.getInstance().time.time
        event.value = BigDecimal.valueOf(1)

        val aggEventType = aggregateEventMap[event.type]
        when {
            aggEventType == null -> {
                aggregateEventMap[event.type] = hashMapOf()
                val map = aggregateEventMap[event.type]
                map!![event.target] = event
            }
            aggEventType.containsKey(event.target) -> {
                aggEventType[event.target]!!.value = aggEventType[event.target]!!.value?.plus(BigDecimal.ONE)
            }
            else -> {
                aggEventType[event.target] = event
            }
        }
    }

    private fun eventsFromDvcEvent(currentEventQueue: List<DVCEvent>) : List<Event> {
        val eventList = mutableListOf<Event>()
        mutableListOf<Event>()
        currentEventQueue.forEach { eventList += Event.fromDVCEvent(it, user, config!!) }
        return eventList
    }

    private fun eventsFromAggregateEventMap(): List<Event> {
        val eventList = mutableListOf<Event>()
        aggregateEventMap.flatMap { it.value.values }.forEach { eventList += Event.fromAggregateEvent(it, user, config) }
        return eventList
    }

    override fun run() {
        flushEvents()
    }
}