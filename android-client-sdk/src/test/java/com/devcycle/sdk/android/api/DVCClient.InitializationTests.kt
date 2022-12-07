package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import junit.framework.AssertionFailedError
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.*
import java.util.concurrent.*

class InitializationTests : DVCClientTestBase() {
    @Test
    fun `onInitialized will throw an error if SDK key is not valid`() {
        val client = createClient("invalid-client-sdk", "https://sdk-api.devcycle.com/")

        try {
            client.onInitialized(object : DVCCallback<String> {
                override fun onSuccess(result: String) {
                    error = AssertionFailedError("Client initialized with invalid SDK key!")
                    calledBack = true
                    countDownLatch.countDown()
                }

                override fun onError(t: Throwable) {
                    Assertions.assertEquals("Only 'mobile', 'dvc_mobile' keys are supported by this API", t.message)
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
    fun `onInitialized will return successfully if SDK key is valid`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        try {
            client.onInitialized(object : DVCCallback<String> {
                override fun onSuccess(result: String) {
                    Assertions.assertEquals("Config loaded", result)
                    Assertions.assertNotNull(client.allVariables())
                    client.allVariables()?.isNotEmpty()?.let { Assertions.assertTrue(it) }
                    calledBack = true
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
    fun `onInitialized will return successfully if enableEdgeDB param is set to true`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), enableEdgeDB = true)

        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests.add(request)
                if (request.path == "/v1/events") {
                    return MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}")
                } else if (request.path!!.contains("/v1/mobileSDKConfig")) {
                    Assertions.assertEquals(true, request.path.toString().endsWith("enableEdgeDB=true"))
                    return MockResponse().setResponseCode(200)
                        .setBody(jacksonObjectMapper().writeValueAsString(config))
                }
                return MockResponse().setResponseCode(404)
            }
        }

        try {
            client.onInitialized(object : DVCCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true
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
}
