package com.devcycle.sdk.android.api

import android.content.Context
import com.devcycle.sdk.android.model.EvalReason
import com.devcycle.sdk.android.model.Event
import com.devcycle.sdk.android.model.PopulatedUser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import java.math.BigDecimal


class EventQueueTests {

    private val mockContext: Context = Mockito.mock(Context::class.java)

    @DelicateCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun `events are aggregated correctly`() {
        val request = Request("some-key", "http://fake.com", "http://fake.com", mockContext)
        val user = PopulatedUser("test")
        val eventQueue = EventQueue(request, { user }, CoroutineScope(Dispatchers.Default), 10000)

        val optInEval = EvalReason("OPT_IN", "Opt-In", "test_opt_in_target_id")
        val defaultEval = EvalReason.defaultReason("User Not Targeted")
        val event1 = Event.fromInternalEvent(
            Event.variableEvent(false, "dummy_key1", optInEval),
            user,
            null
        )
        val event2 = Event.fromInternalEvent(
            Event.variableEvent(false, "dummy_key1", optInEval),
            user,
            null
        )

        val defaultEvent1 = Event.fromInternalEvent(
            Event.variableEvent(true, "dummy_key1", defaultEval),
            user,
            null
        )
        val defaultEvent2 = Event.fromInternalEvent(
            Event.variableEvent(true, "dummy_key1", defaultEval),
            user,
            null
        )

        eventQueue.queueAggregateEvent(event1)
        eventQueue.queueAggregateEvent(event2)
        eventQueue.queueAggregateEvent(defaultEvent1)
        eventQueue.queueAggregateEvent(defaultEvent2)

        val aggregateEvaluatedEvent = eventQueue.aggregateEventMap[Event.Companion.EventTypes.variableEvaluated]?.get("dummy_key1")
        val aggregateDefaultedEvent = eventQueue.aggregateEventMap[Event.Companion.EventTypes.variableDefaulted]?.get("dummy_key1")
        val evalEventMetadata = aggregateEvaluatedEvent?.metaData?.get("eval")
        val defaultEventMetadata = aggregateDefaultedEvent?.metaData?.get("eval")

        Assert.assertEquals(BigDecimal(2), aggregateEvaluatedEvent?.value)
        Assert.assertEquals(BigDecimal(2), aggregateDefaultedEvent?.value)
        Assert.assertEquals(optInEval, evalEventMetadata)
        Assert.assertEquals(defaultEval, defaultEventMetadata)
    }
}