package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import java.util.concurrent.*

class VariableTests : DVCClientTestBase() {
    @Test
    fun `variable calls back when variable value changes`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        try {
            val variable = client.variable("activate-flag", "Not activated")
            variable.onUpdate(object: DVCCallback<Variable<String>> {
                override fun onSuccess(result: Variable<String>) {
                    Assertions.assertEquals("Flag activated!", result.value)
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
    fun `variable calls back when variable value changes using plain function`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        try {
            val variable = client.variable("activate-flag", "Not activated")
            variable.onUpdate {
                Assertions.assertEquals("Flag activated!", it.value)
                calledBack = true
                countDownLatch.countDown()
            }
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
    }

    @Test
    fun `variable calls return the same instance for the same key and default value`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val variable = client.variable("activate-flag", "Not activated")
        val variable2 = client.variable("activate-flag", "Not activated")
        val variable3 = client.variable("activate-flag", "Activated")

        assert(variable === variable2)
        assert(variable !== variable3)

        client.variable("activate-flag", "Test Weak Reference")
        val variable4 = client.variable("activate-flag", "Test Weak Reference")
        assert(variable !== variable4)
    }
}
