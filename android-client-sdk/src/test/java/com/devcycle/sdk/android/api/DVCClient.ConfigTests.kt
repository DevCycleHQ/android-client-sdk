package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.*
import java.lang.reflect.Method
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger


class ConfigTests : DVCClientTestBase() {
    @Test
    fun `ensure config requests cannot happen in parallel`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        val configTwo = generateConfig("wobble", "Second!", Variable.TypeEnum.STRING)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {

                if (request.path == "/v1/events") {
                    return MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}")
                } else if (request.path!!.contains("/v1/mobileSDKConfig")) {
                    return if (request.sequenceNumber == 1) {
                        MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config))
                    } else {
                        MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(configTwo))
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }

        val client = createClient("pretend-its-real", mockWebServer.url("/").toString())

        try {
            client.onInitialized(object : DVCCallback<String> {
                override fun onSuccess(result: String) {
                    client.resetUser(object: DVCCallback<Map<String, Variable<Any>>> {
                        override fun onSuccess(result: Map<String, Variable<Any>>) {
                            Assertions.assertEquals("Flag activated!", result["activate-flag"]?.value.toString())
                        }

                        override fun onError(t: Throwable) {
                            error = t
                            calledBack = true
                            countDownLatch.countDown()
                        }

                    })

                    client.identifyUser(DVCUser.builder().withUserId("new_userid").build(), object: DVCCallback<Map<String, Variable<Any>>> {
                        override fun onSuccess(result: Map<String, Variable<Any>>) {
                            Assertions.assertEquals("Second!", result["wobble"]?.value.toString())
                            calledBack = true
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

    @Test
    fun `ensure config requests are queued and executed later`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        generateDispatcher(config = config)

        val client = createClient("pretend-its-real", mockWebServer.url("/").toString())
        client.onInitialized(object: DVCCallback<String> {
            override fun onSuccess(result: String) {
                val takeRequest = requests.remove()
                takeRequest.path?.contains("nic_test")?.let { Assertions.assertTrue(it) }
            }

            override fun onError(t: Throwable) {
                calledBack = true
                error = t
                countDownLatch.countDown()
            }
        })

        val i = AtomicInteger(0)

        val callback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                Assertions.assertEquals("Flag activated!", result["activate-flag"]?.value.toString())

                if (i.get() == 0) {
                    // If there are no more requests mockwebserver awaits the next response and we're not expecting any more
                    val takeRequest = requests.remove()
                    takeRequest.path?.contains("new_userid5")?.let { Assertions.assertTrue(it) }
                }

                calledBack = true
                i.getAndIncrement()

                if (i.get() == 5) {
                    countDownLatch.countDown()
                }
            }

            override fun onError(t: Throwable) {
                error = t
                calledBack = true
                countDownLatch.countDown()
            }
        }

        try {
            client.identifyUser(DVCUser.builder().withUserId("new_userid").build(), callback)
            client.identifyUser(DVCUser.builder().withUserId("new_userid1").build(), callback)
            client.identifyUser(DVCUser.builder().withUserId("new_userid2").build(), callback)
            client.identifyUser(DVCUser.builder().withUserId("new_userid3").build(), callback)
            client.identifyUser(DVCUser.builder().withUserId("new_userid4").build(), callback)
            client.identifyUser(DVCUser.builder().withUserId("new_userid5").build(), callback)
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(4000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
            Assertions.assertEquals(2, configRequestCount)
        }
    }

    @Test
    fun `refetchConfig uses most recent user data`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        generateDispatcher(config = config)
        val client = createClient("pretend-its-real", mockWebServer.url("/").toString())

        val refetchConfigCallback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                val takeRequest = requests.remove()
                takeRequest.path?.contains("last_userId")?.let { Assertions.assertTrue(it) }
            }

            override fun onError(t: Throwable) {
                error = t
            }
        }
        client.identifyUser(DVCUser.builder().withUserId("new_userid").build())
        client.identifyUser(DVCUser.builder().withUserId("new_userid2").build())
        client.identifyUser(DVCUser.builder().withUserId("last_userId").build())

        // make private refetchConfig callable
        val refetchConfigMethod: Method = DVCClient::class.java.getDeclaredMethod("refetchConfig", Boolean::class.java, 1L::class.javaObjectType, DVCCallback::class.java)
        refetchConfigMethod.isAccessible = true

        // call refetchConfig -> the callback will assert that the config request triggered by this refetch config had the last seen user_id
        refetchConfigMethod.invoke(client, true, 1000L, refetchConfigCallback)

        countDownLatch.await(2000, TimeUnit.MILLISECONDS)
        // only the init and refetchConfig requests should have been sent bc the identifyUser requests should have been skipped in the queue
        Assertions.assertEquals(2, configRequestCount)

    }
}
