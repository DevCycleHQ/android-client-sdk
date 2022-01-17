package com.devcycle.sdk.android.api

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.devcycle.sdk.android.api.DVCClient.Companion.builder
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import junit.framework.AssertionFailedError
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

class DVCClientTests {

    private lateinit var mockWebServer: MockWebServer

    private val mockContext: Context? = Mockito.mock(Context::class.java)
    private val sharedPreferences: SharedPreferences? = Mockito.mock(SharedPreferences::class.java)
    private val editor: SharedPreferences.Editor? = Mockito.mock(SharedPreferences.Editor::class.java)
    private val resources: Resources = Mockito.mock(Resources::class.java)
    private val configuration: Configuration = Mockito.mock(Configuration::class.java)
    private val locales: LocaleList = Mockito.mock(LocaleList::class.java)
    private val packageManager: PackageManager = Mockito.mock(PackageManager::class.java)

    private val packageInfo = mockk<PackageInfo>()

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
        `when`(mockContext.resources).thenReturn(resources)
        `when`(mockContext.resources.configuration).thenReturn(configuration)
        `when`(mockContext.resources.configuration.locales).thenReturn(locales)
        `when`(mockContext.resources.configuration.locales.get(0)).thenReturn(Locale.getDefault())
        `when`(mockContext.packageName).thenReturn("test")
        `when`(mockContext.packageManager).thenReturn(packageManager)
        `when`(mockContext.packageManager.getPackageInfo("test", 0)).thenReturn(packageInfo)


        //every { packageInfo.versionName } returns "1.0"
        every { packageInfo.longVersionCode } returns 1

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
                    Assertions.assertEquals("Only 'mobile' keys are supported by this API", t.message)
                    countDownLatch.countDown()
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
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

        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables["activate-flag"] =
            createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        val config = BucketedUserConfig(variables = variables)

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

        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables["activate-flag"] =
            createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        val config = BucketedUserConfig(variables = variables)

        // init client
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        // call reset
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)).throttleBody(1024, 10, TimeUnit.SECONDS).setHeadersDelay(200L, TimeUnit.MILLISECONDS))

        val variablesTwo: MutableMap<String, Variable<Any>> = HashMap()
        variablesTwo["wobble"] =
            createNewVariable("wobble", "Second!", Variable.TypeEnum.STRING)

        val configTwo = BucketedUserConfig(variables = variablesTwo)

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
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
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
    fun `ensure config requests are queued and executed later`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val client = createClient("pretend-its-real", mockWebServer.url("/").toString())
        client.onInitialized(object: DVCCallback<String> {
            override fun onSuccess(result: String) {
                val takeRequest = mockWebServer.takeRequest()
                takeRequest.path?.contains("nic_test")?.let { Assertions.assertTrue(it) }
            }

            override fun onError(t: Throwable) {
                error = t
                calledBack = true
                countDownLatch.countDown()
            }

        })

        val i = AtomicInteger(0)

        val callback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                Assertions.assertEquals("Flag activated!", result["activate-flag"]?.value.toString())

                if (i.get() == 0) {
                    // If there are no more requests mockwebserver awaits the next response and we're not expecting any more
                    val takeRequest = mockWebServer.takeRequest()
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

        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables["activate-flag"] =
            createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        val config = BucketedUserConfig(variables = variables)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

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
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
            Assertions.assertEquals(2, mockWebServer.requestCount)
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
    fun `variable calls back when variable value changes`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables["activate-flag"] =
            createNewVariable("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        val config = BucketedUserConfig(variables = variables)

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
            countDownLatch.await(200000, TimeUnit.MILLISECONDS)
            mockWebServer.shutdown()
            if (!calledBack) {
                error = AssertionFailedError("Variable did not callback")
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