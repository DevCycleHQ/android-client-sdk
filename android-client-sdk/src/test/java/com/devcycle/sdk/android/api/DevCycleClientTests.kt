package com.devcycle.sdk.android.api

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.LocaleList
import android.util.Log
import com.devcycle.sdk.android.api.DevCycleClient.Companion.builder
import com.devcycle.sdk.android.helpers.TestDVCLogger
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.LogLevel
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule
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
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.Assert.assertNotNull
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class DevCycleClientTests {
    private lateinit var mockWebServer: MockWebServer

    private val objectMapper = jacksonObjectMapper().registerModule(JsonOrgModule())

    private val logger = TestDVCLogger()
    private var configRequestCount = 0
    private var calledBack = false
    private var error: Throwable? = null
    private var countDownLatch = CountDownLatch(1)

    private val mockApplication: Application? = Mockito.mock(Application::class.java)
    private val mockContext: Context? = Mockito.mock(Context::class.java)
    private val sharedPreferences: SharedPreferences? = Mockito.mock(SharedPreferences::class.java)
    private val editor: SharedPreferences.Editor? = Mockito.mock(SharedPreferences.Editor::class.java)
    private val resources: Resources = Mockito.mock(Resources::class.java)
    private val configuration: Configuration = Mockito.mock(Configuration::class.java)
    private val locales: LocaleList = Mockito.mock(LocaleList::class.java)
    private val packageManager: PackageManager = Mockito.mock(PackageManager::class.java)
    private val mockHandler: Handler = Mockito.mock(Handler::class.java)

    private val packageInfo = mockk<PackageInfo>()

    @DelicateCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    val requests = ConcurrentLinkedQueue<RecordedRequest>()

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
        `when`(mockContext.applicationContext).thenReturn(mockApplication)

        var removeCallbacksCalled: Boolean

        val mockPostDelayed = Answer<Void?> { invocation ->
            removeCallbacksCalled = false
            val runnable = Runnable {
                if (!removeCallbacksCalled) (invocation.arguments[0] as Runnable).run()
                countDownLatch.countDown()
            }
            mainThread.schedule(runnable, invocation.arguments[1] as Long, TimeUnit.MILLISECONDS);
            null
        }
        `when`(mockHandler.post(ArgumentMatchers.any(Runnable::class.java))).thenAnswer {
            (it.arguments[0] as? Runnable)?.run()
            true
        }
        `when`(mockHandler.postDelayed(
            ArgumentMatchers.any(Runnable::class.java),
            Mockito.anyLong()
        )).thenAnswer(mockPostDelayed)

        val mockRemoveCallbacks = Answer<Void?> { _ ->
            removeCallbacksCalled = true
            null
        }

        `when`(mockHandler.removeCallbacks(ArgumentMatchers.any(Runnable::class.java))).thenAnswer(mockRemoveCallbacks)
        every { packageInfo.longVersionCode } returns 1

        Dispatchers.setMain(mainThreadSurrogate)

        requests.clear()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        configRequestCount = 0
        calledBack = false
        error = null
        countDownLatch = CountDownLatch(1)
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
        val client = createClient("invalid-client-sdk", "https://sdk-api.devcycle.com/")

        try {
            client.onInitialized(object : DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    error = AssertionFailedError("Client initialized with invalid SDK key!")
                    calledBack = true
                    countDownLatch.countDown()
                }

                override fun onError(t: Throwable) {
                    Assertions.assertEquals("Only 'mobile', 'dvc_mobile' keys are supported by this API. Invalid key: invalid-client-sdk", t.message)
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
        client.close()
    }

    @Test
    fun `onInitialized will return successfully if SDK key is valid`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        try {
            client.onInitialized(object : DevCycleCallback<String> {
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
        client.close()
    }

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
                        MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config))
                    } else {
                        MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(configTwo))
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }

        val client = createClient("pretend-its-real", mockWebServer.url("/").toString())

        try {
            client.onInitialized(object : DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    client.resetUser(object: DevCycleCallback<Map<String, BaseConfigVariable>> {
                        override fun onSuccess(result: Map<String, BaseConfigVariable>) {
                            Assertions.assertEquals("Flag activated!", result["activate-flag"]?.value.toString())
                        }

                        override fun onError(t: Throwable) {
                            error = t
                            calledBack = true
                            countDownLatch.countDown()
                        }

                    })

                    client.identifyUser(DevCycleUser.builder().withUserId("new_userid").build(), object: DevCycleCallback<Map<String, BaseConfigVariable>> {
                        override fun onSuccess(result: Map<String, BaseConfigVariable>) {
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
        client.close()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `ensure config requests are queued and executed later`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        generateDispatcher(config = config)
        val initializeLatch = CountDownLatch(1)

        val client = createClient("pretend-its-real", mockWebServer.url("/").toString(), 100L, false, null, LogLevel.VERBOSE)
        client.onInitialized(object: DevCycleCallback<String> {
            override fun onSuccess(result: String) {
                calledBack = true
                val takeRequest = requests.remove()
                takeRequest.path?.contains("nic_test")?.let { Assertions.assertTrue(it) }
                initializeLatch.countDown()
            }

            override fun onError(t: Throwable) {
                error = t
            }
        })

        waitForOpenLatch(initializeLatch, 2000, TimeUnit.MILLISECONDS)

        val i = AtomicInteger(0)
        val callbackLatch = CountDownLatch(6)

        val callback = object: DevCycleCallback<Map<String, BaseConfigVariable>> {
            override fun onSuccess(result: Map<String, BaseConfigVariable>) {
                Assertions.assertEquals("Flag activated!", result["activate-flag"]?.value.toString())

                i.getAndIncrement()

                callbackLatch.countDown()
            }

            override fun onError(t: Throwable) {
                error = t
            }
        }

        client.identifyUser(DevCycleUser.builder().withUserId("expected_userid1").build(), callback)
        client.identifyUser(DevCycleUser.builder().withUserId("random_user1").build(), callback)
        client.identifyUser(DevCycleUser.builder().withUserId("random_user2").build(), callback)
        client.identifyUser(DevCycleUser.builder().withUserId("random_user3").build(), callback)
        client.identifyUser(DevCycleUser.builder().withUserId("random_user4").build(), callback)
        client.identifyUser(DevCycleUser.builder().withUserId("expected_userid2").build(), callback)

        waitForOpenLatch(callbackLatch, 5000, TimeUnit.MILLISECONDS)

        // initial config request + leading and trailing edge debounce of identify calls
        Assertions.assertEquals(3, configRequestCount)
        requests.forEach {
            if (it.path?.contains("/events") == true) {
                return
            } else {
                // expect only the first and last user id to be sent as a result of the identify calls
                try {
                    // TODO: Figure out why this is inconsistently failing
//                     Assertions.assertEquals(true, it.path?.contains("expected_userid"))
                } catch (e: org.opentest4j.AssertionFailedError) {
                    throw e
                }

            }
        }

        handleFinally(calledBack, error)

        client.close()
    }

    @Test
    fun `refetchConfig uses most recent user data`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        generateDispatcher(config = config)
        val client = createClient("pretend-its-real", mockWebServer.url("/").toString())

        val refetchConfigCallback = object: DevCycleCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                val takeRequest = requests.remove()
                takeRequest.path?.contains("last_userId")?.let { Assertions.assertTrue(it) }
            }

            override fun onError(t: Throwable) {
                error = t
            }
        }
        client.identifyUser(DevCycleUser.builder().withUserId("new_userid").build())
        client.identifyUser(DevCycleUser.builder().withUserId("new_userid2").build())
        client.identifyUser(DevCycleUser.builder().withUserId("last_userId").build())

        // make private refetchConfig callable
        val refetchConfigMethod: Method = DevCycleClient::class.java.getDeclaredMethod(
            "refetchConfig",
            Boolean::class.java,
            1L::class.javaObjectType,
            String::class.java,
            DevCycleCallback::class.java
        )
        refetchConfigMethod.isAccessible = true

        // call refetchConfig -> the callback will assert that the config request triggered by this refetch config had the last seen user_id
        refetchConfigMethod.invoke(client, true, 1000L, "etag", refetchConfigCallback)

        countDownLatch.await(2000, TimeUnit.MILLISECONDS)
        // only the init and refetchConfig requests should have been sent bc the identifyUser requests should have been skipped in the queue
        Assertions.assertEquals(2, configRequestCount)
        client.close()
    }

    @Test
    fun `variable calls back when variable value changes`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        try {
            val variable = client.variable("activate-flag", "Not activated")
            variable.onUpdate(object: DevCycleCallback<Variable<String>> {
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
            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    client.identifyUser(DevCycleUser.builder().withUserId("asdasdas").build())
                }
                override fun onError(t: Throwable) {
                    error = t
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
        client.close()
    }

    @Test
    fun `variable calls back when variable value changes using plain function`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        try {
            val variable = client.variable("activate-flag", "Not activated")
            variable.onUpdate {
                Assertions.assertEquals("Flag activated!", it.value)
                calledBack = true
                countDownLatch.countDown()
            }

            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    client.identifyUser(DevCycleUser.builder().withUserId("asdasdas").build())
                }
                override fun onError(t: Throwable) {
                    error = t
                }
            })
        } catch(t: Throwable) {
            countDownLatch.countDown()
        } finally {
            countDownLatch.await(2000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
        client.close()
    }

    @Test
    fun `variable calls back when variable value has changed for json object`() {
        val config = generateJSONObjectConfig("activate-flag", JSONObject(mapOf("test1" to "value1")))
        val config2 = generateJSONObjectConfig("activate-flag", JSONObject(mapOf("test2" to "value2")))

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config2)))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config2)))

        Assertions.assertEquals(0, mockWebServer.requestCount)

        val client = createClient("dvc-mobile-pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val countDownLatch = CountDownLatch(2)
        val initializedLatch = CountDownLatch(1)

        val variable = client.variable("activate-flag", JSONObject(mapOf("test" to "default")))
        variable.onUpdate {
            countDownLatch.countDown()
        }

        client.onInitialized(object: DevCycleCallback<String> {
            override fun onSuccess(result: String) {
                client.identifyUser(DevCycleUser.builder().withUserId("asdasdas").build())
                initializedLatch.countDown()
            }
            override fun onError(t: Throwable) {
                error = t
            }
        })

        initializedLatch.await(2000, TimeUnit.MILLISECONDS)
        Assertions.assertEquals(variable.value.getString("test1"), "value1")

        countDownLatch.await(3000, TimeUnit.MILLISECONDS)
        Assertions.assertEquals(variable.value.getString("test2"), "value2")

        Assertions.assertEquals(0, countDownLatch.count)
        Assertions.assertEquals(0, initializedLatch.count)

        client.close()
    }

    @Test
    fun `variable does not call back when variable value has not changed for json object`() {
        val config = generateJSONObjectConfig("activate-flag", JSONObject(mapOf("test" to "value")))

        generateDispatcher(config = config)
        val client = createClient("dvc-mobile-pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val countDownLatch = CountDownLatch(2)
        val initializedLatch = CountDownLatch(1)

        val variable = client.variable("activate-flag", JSONObject(mapOf("arg" to "yeah")))
        variable.onUpdate {
            countDownLatch.countDown()
        }

        client.onInitialized(object: DevCycleCallback<String> {
            override fun onSuccess(result: String) {
                client.identifyUser(DevCycleUser.builder().withUserId("test_2").build())
                initializedLatch.countDown()
            }
            override fun onError(t: Throwable) {
                error = t
            }
        })

        initializedLatch.await(2000, TimeUnit.MILLISECONDS)
        countDownLatch.await(3000, TimeUnit.MILLISECONDS)
        Assertions.assertEquals(2, configRequestCount)
        Assertions.assertEquals(1, countDownLatch.count)
        Assertions.assertEquals(0, initializedLatch.count)
        client.close()
    }

    @Test
    fun `variable does not call back when variable value has not changed for json array`() {
        val config = generateJSONArrayConfig("activate-flag", JSONArray(arrayOf(1,2)))

        generateDispatcher(config = config)
        val client = createClient("dvc-mobile-pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val countDownLatch = CountDownLatch(2)
        val initializedLatch = CountDownLatch(1)

        val variable = client.variable("activate-flag", JSONArray(arrayOf(2,3)))
        variable.onUpdate {
            countDownLatch.countDown()
        }

        client.onInitialized(object: DevCycleCallback<String> {
            override fun onSuccess(result: String) {
                client.identifyUser(DevCycleUser.builder().withUserId("test_2").build())
                initializedLatch.countDown()
            }
            override fun onError(t: Throwable) {
                error = t
            }
        })

        initializedLatch.await(2000, TimeUnit.MILLISECONDS)
        countDownLatch.await(3000, TimeUnit.MILLISECONDS)
        Assertions.assertEquals(2, configRequestCount)
        Assertions.assertEquals(1, countDownLatch.count)
        Assertions.assertEquals(0, initializedLatch.count)
        client.close()
    }

    @Test
    fun `variable calls return the same instance for the same key and default value`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val variable = client.variable("activate-flag", "Not activated")
        val variable2 = client.variable("activate-flag", "Not activated")
        val variable3 = client.variable("activate-flag", "Activated")

        assert(variable === variable2)
        assert(variable !== variable3)

        client.variable("activate-flag", "Test Weak Reference")
        val variable4 = client.variable("activate-flag", "Test Weak Reference")
        assert(variable !== variable4)
        client.close()
    }

    @Test
    fun `client uses stored anonymous id if it exists`() {
        `when`(sharedPreferences?.getString(eq("ANONYMOUS_USER_ID"), eq(null))).thenReturn("some-anon-id")

        val user = DevCycleClient::class.java.getDeclaredField("user")
        user.setAccessible(true)

        val client = createClient(user=DevCycleUser.builder().withIsAnonymous(true).build())
        var anonUser: PopulatedUser = user.get(client) as PopulatedUser

        Assertions.assertEquals("some-anon-id", anonUser.userId)
        client.close()
    }

    @Test
    fun `client writes anonymous id to store if it doesn't exist`() {
        val user = DevCycleClient::class.java.getDeclaredField("user")
        user.setAccessible(true)

        val client = createClient(user=DevCycleUser.builder().withIsAnonymous(true).build())
        var anonUser: PopulatedUser = user.get(client) as PopulatedUser

        verify(editor, times(1))?.putString(eq("ANONYMOUS_USER_ID"), eq(anonUser.userId))
        client.close()
    }

    @Test
    fun `identifying a user clears the stored anonymous id`() {
        `when`(sharedPreferences?.getString(anyString(), eq(null))).thenReturn("some-anon-id")

        val client = createClient(user=DevCycleUser.builder().withIsAnonymous(true).build())
        val newUser = DevCycleUser.builder().withUserId("123").withIsAnonymous(false).build()
        val callback = object: DevCycleCallback<Map<String, BaseConfigVariable>> {
            override fun onSuccess(result: Map<String, BaseConfigVariable>) {
                verify(editor, times(1))?.remove(eq("ANONYMOUS_USER_ID"))
            }
            override fun onError(t: Throwable) {}
        }
        client.identifyUser(newUser, callback)
        client.close()
    }

    @Test
    fun `resetting the user updates the stored anonymous id`() {
        `when`(sharedPreferences?.getString(anyString(), eq(null))).thenReturn("some-anon-id")
        val user = DevCycleClient::class.java.getDeclaredField("latestIdentifiedUser")
        user.setAccessible(true)

        val client = createClient(user=DevCycleUser.builder().withIsAnonymous(true).build())
        val callback = object: DevCycleCallback<Map<String, BaseConfigVariable>> {
            override fun onSuccess(result: Map<String, BaseConfigVariable>) {
                var anonUser: PopulatedUser = user.get(client) as PopulatedUser
                verify(editor, times(1))?.putString(eq("ANONYMOUS_USER_ID"), eq(anonUser.userId))
                // Assertions to check non-null fields
                assertNotNull("appVersion should not be null", anonUser.appVersion)
                assertNotNull("appBuild should not be null", anonUser.appBuild)
                assertNotNull("language should not be null", anonUser.language)
            }
            override fun onError(t: Throwable) {}
        }
        client.resetUser(callback)
        client.close()
    }

    @Test
    fun `events are flushed with delay`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}"))
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}"))

        val flushInMs = 100L
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs)

        try {
            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true

                    Thread.sleep(1500L)

                    client.track(DevCycleEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .withDate(Date())
                        .build())

                    Thread.sleep(1000L)

                    val logs = logger.logs

                    val searchString = "DevCycle Flushed 1 Events."

                    val filteredLogs = logs.filter { it.second.contains(searchString)}

                    Assertions.assertEquals(filteredLogs.size, 1)
                    Assertions.assertEquals(filteredLogs[0].first, LogLevel.INFO.value)
                    Assertions.assertEquals(filteredLogs[0].second, searchString)

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
            countDownLatch.await(5000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
        client.close()
    }

    @Test
    fun `automatic events are not tracked when disableAutomaticEventLogging used`() {
        Thread.sleep(3000L)
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))


        val flushInMs = 100L
        val options = DevCycleOptions.builder()
            .flushEventsIntervalMs(flushInMs)
            .disableAutomaticEventLogging(true)
            .apiProxyUrl(mockWebServer.url("/").toString())
            .eventsApiProxyUrl(mockWebServer.url("/").toString())
            .build()

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs, false ,null, LogLevel.DEBUG, options)

        try {
            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true

                    client.variable("activate-flag", "Not activated")
                    client.variableValue("activate-flag", "Not activated")
                    client.variable("activate-flag", "Activated")
                    client.variableValue("activate-flag", "Activated")

                    client.track(DevCycleEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .withDate(Date())
                        .build())

                    client.flushEvents()

                    Thread.sleep(150L)

                    val logs = logger.logs

                    val searchString = "DevCycle Flush 1 Events"

                    val filteredLogs = logs.filter { it.second.contains(searchString)}

                    Assertions.assertEquals(filteredLogs.size, 1)
                    Assertions.assertEquals(filteredLogs[0].second, searchString)

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
            countDownLatch.await(5000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
        client.close()
    }

    @Test
    fun `custom events are not tracked when disableCustomEvents is used`() {
        Thread.sleep(500L)
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))


        val flushInMs = 100L
        val options = DevCycleOptions.builder()
            .flushEventsIntervalMs(flushInMs)
            .disableCustomEventLogging(true)
            .apiProxyUrl(mockWebServer.url("/").toString())
            .eventsApiProxyUrl(mockWebServer.url("/").toString())
            .build()

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs, false ,null, logLevel = LogLevel.INFO, options)

        try {
            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true
                    client.track(DevCycleEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .withDate(Date())
                        .build())

                    client.track(DevCycleEvent.builder()
                        .withType("customEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .withDate(Date())
                        .build())

                    client.track(DevCycleEvent.builder()
                        .withType("variableEvaluated")
                        .withMetaData(mapOf("test" to "value"))
                        .withDate(Date())
                        .build())

                    client.variable("activate-flag", "Activated")
                    client.variableValue("activate-flag", "Activated")


                    Thread.sleep(150L)

                    val logs = logger.logs

                    val searchString = "DevCycle Flush 1 Events"

                    val filteredLogs = logs.filter { it.second.contains(searchString)}

                    Assertions.assertEquals(filteredLogs.size, 1)
                    Assertions.assertEquals(filteredLogs[0].second, searchString)

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
            countDownLatch.await(5000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
        client.close()
    }

    @Test
    fun `events are flushed with delay to batch up events`() {
        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)
        generateDispatcher(config = config)

        val flushInMs = 100L
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs)

        try {
            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true

                    client.track(DevCycleEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    client.track(DevCycleEvent.builder()
                        .withType("newTestEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    Thread.sleep(500L)

                    val logs = logger.logs

                    val searchString = "DevCycle Flushed 2 Events."

                    val filteredLogs = logs.filter { it.second.contains(searchString) }

                    Assertions.assertEquals(filteredLogs.size, 1)
                    Assertions.assertEquals(filteredLogs[0].first, LogLevel.INFO.value)
                    Assertions.assertEquals(filteredLogs[0].second, searchString)

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
            countDownLatch.await(5000, TimeUnit.MILLISECONDS)
            handleFinally(calledBack, error)
        }
        client.close()
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
                        .setBody(objectMapper.writeValueAsString(config))
                }
                return MockResponse().setResponseCode(404)
            }
        }

        try {
            client.onInitialized(object : DevCycleCallback<String> {
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
        client.close()
    }

    @Test
    fun `close method will flush and then block events`() {
        var calledBack = false
        var error: Throwable? = null

        val countDownLatch = CountDownLatch(1)

        val config = generateConfig("activate-flag", "Flag activated!", Variable.TypeEnum.STRING)

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(config)))

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}"))

        val flushInMs = 100L
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString(), flushInMs)

        try {
            client.onInitialized(object: DevCycleCallback<String> {
                override fun onSuccess(result: String) {
                    calledBack = true


                    client.track(DevCycleEvent.builder()
                        .withType("testEvent")
                        .withMetaData(mapOf("test" to "value"))
                        .build())

                    client.close(object: DevCycleCallback<String>{
                        override fun onSuccess(result: String) {
                            client.track(DevCycleEvent.builder()
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
        client.close()
    }

    @Test
    fun `variable calls return default value with a null config`() {
        val config = BucketedUserConfig()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config)))

        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val variable = client.variable("activate-flag", "Not activated")
        val varValue = client.variableValue("activate-flag", "Not activated")
        val variable2 = client.variable("activate-flag", "Activated")
        val varValue2 = client.variableValue("activate-flag", "Activated")

        assert(variable !== variable2)
        assert(variable.value === "Not activated")
        assert(varValue === "Not activated")
        assert(variable.isDefaulted == true)
        assert(variable2.value === "Activated")
        assert(varValue2 === "Activated")
        assert(variable2.isDefaulted == true)
    }

    @Test
    fun `client initializes successfully, config is still parsed correctly with an extra root level field that is ignored`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"ignore-me\": \"whoa there!\",\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +
                "      \"_id\": \"2\",\n" +
                "      \"key\": \"test-feature-number\",\n" +
                "      \"type\": \"Number\",\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +
                "      \"_id\": \"3\",\n" +
                "      \"key\": \"test-feature-string\",\n" +
                "      \"type\": \"String\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            assert(boolVar.value)
            assert(boolVar.isDefaulted == false)
            // value doesn't get updated
            assert(!boolValue)

            assert(numVar.value == 42)
            assert(numVar.isDefaulted == false)
            // value doesn't get updated
            assert(numValue == 0)

            assert(strVar.value == "it works!")
            assert(strVar.isDefaulted == false)
            // value doesn't get updated
            assert(strValue == "Not activated")

            val expectedJSON = JSONObject()
            expectedJSON.put("test", "feature")
            assert(jsonVar.value.toString() == expectedJSON.toString())
            assert(jsonVar.isDefaulted == false)
            // value doesn't get updated
            assert(jsonValue.toString() !== expectedJSON.toString())
        }
    }

    @Test
    fun `client initializes successfully, config is still parsed correctly with an extra field in the variables that is ignored`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +      // Extra ignored field
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +
                "      \"_id\": \"2\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +      // Extra ignored field
                "      \"key\": \"test-feature-number\",\n" +
                "      \"type\": \"Number\",\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +
                "      \"_id\": \"3\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +      // Extra ignored field
                "      \"key\": \"test-feature-string\",\n" +
                "      \"type\": \"String\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +      // Extra ignored field
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            assert(boolVar.value)
            assert(boolVar.isDefaulted == false)
            // value doesn't get updated
            assert(!boolValue)

            assert(numVar.value == 42)
            assert(numVar.isDefaulted == false)
            // value doesn't get updated
            assert(numValue == 0)

            assert(strVar.value == "it works!")
            assert(strVar.isDefaulted == false)
            // value doesn't get updated
            assert(strValue == "Not activated")

            val expectedJSON = JSONObject()
            expectedJSON.put("test", "feature")
            assert(jsonVar.value.toString() == expectedJSON.toString())
            assert(jsonVar.isDefaulted == false)
            // value doesn't get updated
            assert(jsonValue.toString() !== expectedJSON.toString())
        }
    }

    @Test
    fun `client initializes successfully, config is still parsed correctly with an extra root field and extra field in the environments that is ignored`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +
                "      \"_id\": \"2\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +
                "      \"key\": \"test-feature-number\",\n" +
                "      \"type\": \"Number\",\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +
                "      \"_id\": \"3\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +
                "      \"key\": \"test-feature-string\",\n" +
                "      \"type\": \"String\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"_feature\": \"test_feature_id_999\",\n" +
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  },\n" +
                "  \"feature_ids\": {\n" +                                      // Extra ignored field
                "    \"test_feature_id_999\": \"test_feature_id_999\"\n" +
                "  },\n" +
                "  \"environments\": {\n" +
                "    \"_id\": \"test_environment_id_999\",\n" +
                "    \"key\": \"test_environment_key_999\",\n" +
                "    \"name\": \"test_environment_name_999\"\n" +               // Extra ignored field
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            assert(boolVar.value)
            assert(boolVar.isDefaulted == false)
            // value doesn't get updated
            assert(!boolValue)

            assert(numVar.value == 42)
            assert(numVar.isDefaulted == false)
            // value doesn't get updated
            assert(numValue == 0)

            assert(strVar.value == "it works!")
            assert(strVar.isDefaulted == false)
            // value doesn't get updated
            assert(strValue == "Not activated")

            val expectedJSON = JSONObject()
            expectedJSON.put("test", "feature")
            assert(jsonVar.value.toString() == expectedJSON.toString())
            assert(jsonVar.isDefaulted == false)
            // value doesn't get updated
            assert(jsonValue.toString() !== expectedJSON.toString())
        }
    }

    @Test
    fun `client receives invalid config, all variables default with an undefined variable key for any variable in config`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +
                "      \"_id\": \"2\",\n" +
                "      \"key\": \"test-feature-number\",\n" +
                "      \"type\": \"Number\",\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +                // No Variable key
                "      \"_id\": \"3\",\n" +
                "      \"type\": \"String\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            // Expect Client to have all variables default
            assert(!boolVar.value)
            assert(boolVar.isDefaulted == true)
            assert(!boolValue)

            assert(numVar.value == 0)
            assert(numVar.isDefaulted == true)
            assert(numValue == 0)

            assert(strVar.value == "Not activated")
            assert(strVar.isDefaulted == true)
            assert(strValue == "Not activated")

            assert(jsonVar.value.toString() == defaultJSON.toString())
            assert(jsonVar.isDefaulted == true)
            assert(jsonValue.toString() == defaultJSON.toString())
        }
    }

    @Test
    fun `client receives invalid config all variables default with an undefined variable type for any variable in config`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +
                "      \"_id\": \"2\",\n" +
                "      \"key\": \"test-feature-number\",\n" +
                "      \"type\": \"Number\",\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +                // No Variable type
                "      \"_id\": \"3\",\n" +
                "      \"key\": \"test-feature-string\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            // Expect Client to have all variables default
            assert(!boolVar.value)
            assert(boolVar.isDefaulted == true)
            assert(!boolValue)

            assert(numVar.value == 0)
            assert(numVar.isDefaulted == true)
            assert(numValue == 0)

            assert(strVar.value == "Not activated")
            assert(strVar.isDefaulted == true)
            assert(strValue == "Not activated")

            assert(jsonVar.value.toString() == defaultJSON.toString())
            assert(jsonVar.isDefaulted == true)
            assert(jsonValue.toString() == defaultJSON.toString())
        }
    }

    @Test
    fun `client receives invalid config, all variables default with a null variable key for any variable in config`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +            // null Variable key
                "      \"_id\": \"2\",\n" +
                "      \"key\": null,\n" +
                "      \"type\": \"Number\",\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +
                "      \"_id\": \"3\",\n" +
                "      \"key\": \"test-feature-string\",\n" +
                "      \"type\": \"String\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            // Expect Client to have all variables default
            assert(!boolVar.value)
            assert(boolVar.isDefaulted == true)
            assert(!boolValue)

            assert(numVar.value == 0)
            assert(numVar.isDefaulted == true)
            assert(numValue == 0)

            assert(strVar.value == "Not activated")
            assert(strVar.isDefaulted == true)
            assert(strValue == "Not activated")

            assert(jsonVar.value.toString() == defaultJSON.toString())
            assert(jsonVar.isDefaulted == true)
            assert(jsonValue.toString() == defaultJSON.toString())
        }
    }

    @Test
    fun `client receives invalid config, all variables default with a null variable type for any variable in config`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{\n" +
                "  \"variables\": {\n" +
                "    \"test-feature\": {\n" +
                "      \"_id\": \"1\",\n" +
                "      \"key\": \"test-feature\",\n" +
                "      \"type\": \"Boolean\",\n" +
                "      \"value\": true\n" +
                "    },\n" +
                "    \"test-feature-number\": {\n" +            // null Variable type
                "      \"_id\": \"2\",\n" +
                "      \"key\": \"test-feature-number\",\n" +
                "      \"type\": null,\n" +
                "      \"value\": 42\n" +
                "    },\n" +
                "    \"test-feature-string\": {\n" +
                "      \"_id\": \"3\",\n" +
                "      \"key\": \"test-feature-string\",\n" +
                "      \"type\": \"String\",\n" +
                "      \"value\": \"it works!\"\n" +
                "    },\n" +
                "    \"test-feature-json\": {\n" +
                "      \"_id\": \"4\",\n" +
                "      \"key\": \"test-feature-json\",\n" +
                "      \"type\": \"JSON\",\n" +
                "      \"value\": { \"test\": \"feature\"}\n" +
                "    }\n" +
                "  }\n" +
                "}"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            // Expect Client to have all variables default
            assert(!boolVar.value)
            assert(boolVar.isDefaulted == true)
            assert(!boolValue)

            assert(numVar.value == 0)
            assert(numVar.isDefaulted == true)
            assert(numValue == 0)

            assert(strVar.value === "Not activated")
            assert(strVar.isDefaulted == true)
            assert(strValue === "Not activated")

            assert(jsonVar.value === defaultJSON)
            assert(jsonVar.isDefaulted == true)
            assert(jsonValue === defaultJSON)
        }
    }

    @Test
    fun `client receives invalid config, client uses default`() {
        val defaultJSON = JSONObject()
        defaultJSON.put("foo", "bar")

        val configString = "{"

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(configString))
        val client = createClient("pretend-its-a-real-sdk-key", mockWebServer.url("/").toString())

        val jsonVar = client.variable("test-feature-json", defaultJSON)
        val jsonValue = client.variableValue("test-feature-json", defaultJSON)
        val boolVar = client.variable("test-feature", false)
        val boolValue = client.variableValue("test-feature", false)
        val numVar = client.variable("test-feature-number", 0)
        val numValue = client.variableValue("test-feature-number", 0)
        val strVar = client.variable("test-feature-string", "Not activated")
        val strValue = client.variableValue("test-feature-string", "Not activated")

        try {
            client.onInitialized(object: DevCycleCallback<String> {
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

            // Expect Client to have failed initialization, all variables should default
            assert(!boolVar.value)
            assert(boolVar.isDefaulted == true)
            assert(!boolValue)

            assert(numVar.value == 0)
            assert(numVar.isDefaulted == true)
            assert(numValue == 0)

            assert(strVar.value === "Not activated")
            assert(strVar.isDefaulted == true)
            assert(strValue === "Not activated")

            assert(jsonVar.value === defaultJSON)
            assert(jsonVar.isDefaulted == true)
            assert(jsonValue === defaultJSON)
        }
    }

    private fun handleFinally(
        calledBack: Boolean,
        error: Throwable?,
    ) {
        if (!calledBack) {
            fail(AssertionFailedError("Client did not initialize"))
        }
        if (error != null) {
            fail(error)
        }
    }

    private fun createClient(
        sdkKey: String = "pretend-its-a-real-sdk-key",
        mockUrl: String = mockWebServer.url("/").toString(),
        flushInMs: Long = 10000L,
        enableEdgeDB: Boolean = false,
        user: DevCycleUser? = null,
        logLevel: LogLevel = LogLevel.DEBUG,
        options: DevCycleOptions? = null,
    ): DevCycleClient {
        val builder = builder()
            .withContext(mockContext!!)
            .withHandler(mockHandler)
            .withUser(user ?: DevCycleUser.builder().withUserId("nic_test").build())
            .withSDKKey(sdkKey)
            .withLogger(logger)
            .withLogLevel(logLevel)
            .withOptions(
                options ?: DevCycleOptions.builder()
                    .flushEventsIntervalMs(flushInMs)
                    .enableEdgeDB(enableEdgeDB)
                    .apiProxyUrl(mockUrl)
                    .eventsApiProxyUrl(mockUrl)
                    .build()
            )

        return builder.build()
    }

    private fun generateConfig(key: String, value: String, type: Variable.TypeEnum): BucketedUserConfig {
        val variables: MutableMap<String, BaseConfigVariable> = HashMap()
        variables[key] = createNewStringVariable(key, value, type)
        val sse = SSE()
        sse.url = "https://www.bread.com"
        return BucketedUserConfig(variables = variables, sse=sse)
    }

    private fun generateJSONObjectConfig(key: String, value: JSONObject): BucketedUserConfig {
        val variables: MutableMap<String, BaseConfigVariable> = HashMap()
        variables[key] = createNewJSONObjectVariable(key, value, Variable.TypeEnum.JSON)
        val sse = SSE()
        sse.url = "https://www.bread.com"
        return BucketedUserConfig(variables = variables, sse=sse)
    }

    private fun generateJSONArrayConfig(key: String, value: JSONArray): BucketedUserConfig {
        val variables: MutableMap<String, BaseConfigVariable> = HashMap()
        variables[key] = createNewJSONArrayVariable(key, value, Variable.TypeEnum.JSON)
        val sse = SSE()
        sse.url = "https://www.bread.com"
        return BucketedUserConfig(variables = variables, sse=sse)
    }

    private fun createNewStringVariable(key: String, value: String, type: Variable.TypeEnum): StringConfigVariable {
        return StringConfigVariable(
            id = UUID.randomUUID().toString(),
            key = key,
            value = value,
            type = type,
            evalReason = null
        )
    }

    private fun createNewJSONObjectVariable(key: String, value: JSONObject, type: Variable.TypeEnum): JSONObjectConfigVariable {
        return JSONObjectConfigVariable(
            id = UUID.randomUUID().toString(),
            key = key,
            value = value,
            type = type,
            evalReason = null
        )
    }

    private fun createNewJSONArrayVariable(key: String, value: JSONArray, type: Variable.TypeEnum): JSONArrayConfigVariable {
        return JSONArrayConfigVariable(
            id = UUID.randomUUID().toString(),
            key = key,
            value = value,
            type = type,
            evalReason = null
        )
    }

    private fun generateDispatcher(routes: List<Pair<String, MockResponse>>? = null, config: BucketedUserConfig? = null) {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (routes == null) {
                    requests.add(request)
                    if (request.path == "/v1/events") {
                        return MockResponse().setResponseCode(201).setBodyDelay(200, TimeUnit.MILLISECONDS).setBody("{\"message\": \"Success\"}")
                    } else if (request.path!!.contains("/v1/mobileSDKConfig")) {
                        configRequestCount++
                        Assertions.assertEquals(false, request.path.toString().contains("enableEdgeDB"))
                        return MockResponse().setResponseCode(200).setBodyDelay(400, TimeUnit.MILLISECONDS).setBody(objectMapper.writeValueAsString(config))
                    }
                } else {
                    for (route: Pair<String, MockResponse> in routes) {
                        if (request.path == route.first) {
                            return route.second
                        }
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }
    }

    private fun waitForOpenLatch(latch: CountDownLatch, timeout: Long, units: TimeUnit) {
        latch.await(timeout, units)
        Assertions.assertEquals(0, latch.count)
    }
}

private val mainThread: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
