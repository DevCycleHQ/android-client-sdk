package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.util.concurrent.*
import kotlin.streams.toList

class EventTests : DVCClientTestBase() {
    @Test
    fun `events are flushed with delay`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}"))
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}"))

        val flushInMs = 100L
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs)

        try {
            client.onInitialized(object: DVCCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true

                    Thread.sleep(1500L)

                    client.track(DVCEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    Thread.sleep(1000L)

                    val logs = tree.logs

                    val searchString = "DVC Flushed 1 Events."

                    Assertions.assertEquals(2, logs.stream().filter { l -> l.message == searchString }.toList().size)

                    countDownLatch.countDown()
                }

                override fun onError(t: Throwable) {
                    error = t
                    calledBack = true
                    countDownLatch.countDown()
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
    }

    @Test
    fun `events are flushed with delay to batch up events`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        generateDispatcher(config = config)

        val flushInMs = 100L
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs)

        try {
            client.onInitialized(object: DVCCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true

                    client.track(DVCEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    client.track(DVCEvent.builder()
                        .withType("newTestEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    Thread.sleep(250L)

                    val logs = tree.logs

                    val searchString = "DVC Flushed 2 Events."

//                    Assertions.assertEquals(1, logs.stream().filter { l -> l.message.startsWith("DVC Flushed") }.toList().size)
                    Assertions.assertEquals(1, logs.stream().filter { l -> l.message == searchString }.toList().size)

                    countDownLatch.countDown()
                }

                override fun onError(t: Throwable) {
                    error = t
                    calledBack = true
                    countDownLatch.countDown()
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(3000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
    }

    @Test
    fun `close method will flush and then block events`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}"))

        val flushInMs = 100L
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs)

        try {
            client.onInitialized(object: DVCCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true


                    client.track(DVCEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    client.close(object: DVCCallback<String>{
                        override fun onSuccess(result: String) {
                            client.track(DVCEvent.builder()
                                .withType("newTestEvent")
                                .withMetaData(mapOf("test2" to "value"))
                                .build())
                            val configRequest: RecordedRequest = mockWebServer.takeRequest()
                            val eventsRequest: RecordedRequest = mockWebServer.takeRequest()

                            val loggedEvents: String = eventsRequest.body.readUtf8()
                            val eventsReqObj = JSONObject(loggedEvents)
                            Mockito.spy(eventsReqObj)
                            val events: JSONArray = eventsReqObj.get("events") as JSONArray
                            Assertions.assertEquals(2, events.length())
                            Assertions.assertEquals("userConfig", events.getJSONObject(0).get("type"))
                            Assertions.assertEquals("testEvent", events.getJSONObject(1).get("customType"))
                            countDownLatch.countDown()
                        }
                        override fun onError(t: Throwable) {
                            error = t
                            calledBack = true
                            countDownLatch.countDown()
                        }
                    })
                }

                override fun onError(t: Throwable) {
                    error = t
                    calledBack = true
                    countDownLatch.countDown()
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
    }
}
