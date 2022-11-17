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
import com.devcycle.sdk.android.api.DVCClient.Companion.builder
import com.devcycle.sdk.android.helpers.TestTree
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.DVCSharedPrefs
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
import kotlin.streams.toList


class DVCClientTests {

    private lateinit var mockWebServer: MockWebServer

    private val tree = TestTree()
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

    @Test
    fun `client uses stored anonymous id if it exists`() {
        `when`(sharedPreferences?.getString(eq("ANONYMOUS_USER_ID"), eq(null))).thenReturn("some-anon-id")

        val user = DVCClient::class.java.getDeclaredField("user")
        user.setAccessible(true)

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        var anonUser: PopulatedUser = user.get(client) as PopulatedUser

        Assertions.assertEquals("some-anon-id", anonUser.userId)
    }

    @Test
    fun `client writes anonymous id to store if it doesn't exist`() {
        val user = DVCClient::class.java.getDeclaredField("user")
        user.setAccessible(true)

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        var anonUser: PopulatedUser = user.get(client) as PopulatedUser

        verify(editor, times(1))?.putString(eq("ANONYMOUS_USER_ID"), eq(anonUser.userId))
    }

    @Test
    fun `identifying a user clears the stored anonymous id`() {
        `when`(sharedPreferences?.getString(anyString(), eq(null))).thenReturn("some-anon-id")

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        val newUser = DVCUser.builder().withUserId("123").withIsAnonymous(false).build()
        val callback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                verify(editor, times(1))?.remove(eq("ANONYMOUS_USER_ID"))
            }
            override fun onError(t: Throwable) {}
        }
        client.identifyUser(newUser, callback)
    }

    @Test
    fun `resetting the user updates the stored anonymous id`() {
        `when`(sharedPreferences?.getString(anyString(), eq(null))).thenReturn("some-anon-id")
        val user = DVCClient::class.java.getDeclaredField("latestIdentifiedUser")
        user.setAccessible(true)

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        val callback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                var anonUser: PopulatedUser = user.get(client) as PopulatedUser
                verify(editor, times(1))?.putString(eq("ANONYMOUS_USER_ID"), eq(anonUser.userId))
            }
            override fun onError(t: Throwable) {}
        }
        client.resetUser(callback)
    }

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
        user: DVCUser? = null
    ): DVCClient {
        val builder = builder()
            .withContext(mockContext!!)
            .withHandler(mockHandler)
            .withUser(user ?: DVCUser.builder().withUserId("nic_test").build())
            .withEnvironmentKey(sdkKey)
            .withLogger(tree)
            .withApiUrl(mockUrl)
            .withOptions(
                DVCOptions.builder()
                    .flushEventsIntervalMs(flushInMs)
                    .enableEdgeDB(enableEdgeDB)
                    .build()
            )

        return builder.build()
    }

    private fun generateConfig(key: String, value: String, type: Variable.TypeEnum): BucketedUserConfig {
        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables[key] = createNewVariable(key, value, type)
        val sse = SSE()
        sse.url = "https://www.bread.com"
        return BucketedUserConfig(variables = variables, sse=sse)
    }

    private fun <T> createNewVariable(key: String, value: T, type: Variable.TypeEnum): Variable<T> {
        return Variable(
            id = UUID.randomUUID().toString(),
            key = key,
            value = value,
            type = type
        )
    }

    val requests = ConcurrentLinkedQueue<RecordedRequest>()

    private fun generateDispatcher(routes: List<Pair<String, MockResponse>>? = null, config: BucketedUserConfig? = null) {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (routes == null) {
                    requests.add(request)
                    if (request.path == "/v1/events") {
                        return MockResponse().setResponseCode(201).setBody("{\"message\": \"Success\"}")
                    } else if (request.path!!.contains("/v1/mobileSDKConfig")) {
                        configRequestCount++
                        Assertions.assertEquals(false, request.path.toString().contains("enableEdgeDB"))
                        return MockResponse().setResponseCode(200).setBody(jacksonObjectMapper().writeValueAsString(config))
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
}

private val mainThread: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
