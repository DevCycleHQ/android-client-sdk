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
import com.devcycle.sdk.android.api.*
import com.devcycle.sdk.android.helpers.TestTree
import com.devcycle.sdk.android.model.*
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
import org.junit.jupiter.api.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import java.util.*
import java.util.concurrent.*

abstract class DVCClientTestBase {
    protected var configRequestCount = 0
    protected var tree: TestTree = TestTree()

    private val mockContext: Context = Mockito.mock(Context::class.java)
    private val mockHandler: Handler = Mockito.mock(Handler::class.java)
    protected lateinit var mockWebServer: MockWebServer

    protected var calledBack = false
    protected var error: Throwable? = null
    protected var countDownLatch = CountDownLatch(1)

    private val mockApplication: Application? = Mockito.mock(Application::class.java)
    protected val sharedPreferences: SharedPreferences? = Mockito.mock(SharedPreferences::class.java)
    protected val editor: SharedPreferences.Editor? = Mockito.mock(SharedPreferences.Editor::class.java)
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

    fun handleFinally(
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

    fun createClient(
        sdkKey: String = "pretend-its-a-real-sdk-key",
        mockUrl: String = mockWebServer.url("/").toString(),
        flushInMs: Long = 10000L,
        enableEdgeDB: Boolean = false,
        user: DVCUser? = null
    ): DVCClient {
        val builder = DVCClient.builder()
            .withContext(mockContext)
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

    fun generateConfig(key: String, value: String, type: Variable.TypeEnum): BucketedUserConfig {
        val variables: MutableMap<String, Variable<Any>> = HashMap()
        variables[key] = createNewVariable(key, value, type)
        val sse = SSE()
        sse.url = "https://www.bread.com"
        return BucketedUserConfig(variables = variables, sse=sse)
    }

    fun <T> createNewVariable(key: String, value: T, type: Variable.TypeEnum): Variable<T> {
        return Variable(
            id = UUID.randomUUID().toString(),
            key = key,
            value = value,
            type = type
        )
    }

    val requests = ConcurrentLinkedQueue<RecordedRequest>()

    fun generateDispatcher(routes: List<Pair<String, MockResponse>>? = null, config: BucketedUserConfig? = null) {
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