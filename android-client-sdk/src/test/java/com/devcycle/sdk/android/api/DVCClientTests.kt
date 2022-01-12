package com.devcycle.sdk.android.api

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import com.devcycle.sdk.android.api.DVCClient.Companion.builder
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import junit.framework.AssertionFailedError
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class DVCClientTests {

    private lateinit var mockWebServer: MockWebServer

    private val mockContext: Context? = Mockito.mock(Context::class.java)

    private val sharedPreferences: SharedPreferences? = Mockito.mock(SharedPreferences::class.java)

    private val editor: SharedPreferences.Editor? = Mockito.mock(SharedPreferences.Editor::class.java)

    @DelicateCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    @BeforeEach
    fun setup() {
        `when`(mockContext!!.getString(anyInt())).thenReturn("Some value")
        `when`(mockContext.getSharedPreferences("Some value", MODE_PRIVATE)).thenReturn(
            sharedPreferences
        )

        `when`(sharedPreferences!!.edit()).thenReturn(editor)

        `when`(editor!!.putString(anyString(), anyString())).thenReturn(editor)

        Dispatchers.setMain(mainThreadSurrogate)

        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()

        mockWebServer.shutdown()
    }

    @Test
    fun `onInitialized will throw an error if SDK key is not valid`() {
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val client = createClient("invalid-client-sdk", "https://sdk-api.devcycle.com/")

        try {
            client.onInitialized(object : DVCCallback<String> {
                override fun onSuccess(result: String) {
                    error = AssertionFailedError("Client initialized with invalid SDK key!")
                    countDownLatch.countDown()
                }

                override fun onError(t: Throwable) {
                    Assertions.assertEquals("DVCClient: DevCycle SDK Failed to Initialize!", t.message)
                    countDownLatch.countDown()
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(200000, TimeUnit.MILLISECONDS)
            if (error != null) {
                fail(error)
            }
        }
    }

    @Test
    fun `onInitialized will return successfully if SDK key is valid`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = BucketedUserConfig()
        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables["activate-flag"] =
            createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        config.variables = variables

        //mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\": \"Only 'mobile' keys are supported by this API\"}"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        val client = createClient("mobile-c06ea707-791d-4978-a91d-c863616b7f51", mockWebServer.url("/").toString())

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
            countDownLatch.await(200000, TimeUnit.MILLISECONDS)
            mockWebServer.shutdown()
            if (!calledBack) {
                error = AssertionFailedError("Client did not initialize")
            }
            if (error != null) {
                fail(error)
            }
        }
    }

    @Test
    fun `ensure config requests cannot happen in parallel`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = BucketedUserConfig()
        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables["activate-flag"] =
            createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        config.variables = variables

        // init client
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        // call reset
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)).throttleBody(1024, 10, TimeUnit.SECONDS).setHeadersDelay(200L, TimeUnit.MILLISECONDS))

        val configTwo = BucketedUserConfig()
        val variablesTwo: MutableMap<String, Variable<Any>> = HashMap()
        variablesTwo["wobble"] =
            createNewVariable("wobble", "Second!", Variable.TypeEnum.STRING)

        configTwo.variables = variablesTwo

        // call identify
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(configTwo)))

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
            countDownLatch.await(200000, TimeUnit.MILLISECONDS)
            mockWebServer.shutdown()
            if (!calledBack) {
                error = AssertionFailedError("Client did not initialize")
            }
            if (error != null) {
                fail(error)
            }
        }
    }

    private fun createClient(sdkKey: String, mockUrl: String): DVCClient {
        val builder = builder()
            .withContext(mockContext!!)
            .withUser(
                DVCUser.builder()
                    .withUserId("nic_test")
                    .build()
            )
            //.withDispatcher(testScope.coroutineContext)
            .withEnvironmentKey(sdkKey)
            .withApiUrl(mockUrl)

        return builder.build()
    }

    private fun <T> createNewVariable(key: String, value: T, type: Variable.TypeEnum): Variable<Any> {
        val variable: Variable<Any> = Variable()
        variable.id = UUID.randomUUID().toString()
        variable.type = type
        variable.value = value
        variable.key = key
        return variable
    }
}