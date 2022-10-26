package com.devcycle.sdk.android.api

import android.app.Activity
import android.os.Handler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class DVCLifecycleCallbacksTests {

    private val mockActivity: Activity = mock(Activity::class.java)
    private val mockHandler: Handler = mock(Handler::class.java)
    private var countDownLatch = CountDownLatch(1)


    @BeforeEach
    fun setup() {
        countDownLatch = CountDownLatch(1)
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
        `when`(mockHandler.postDelayed(any(Runnable::class.java), anyLong())).thenAnswer(mockPostDelayed)

        val mockRemoveCallbacks = Answer<Void?> { _ ->
            removeCallbacksCalled = true
            null
        }
        `when`(mockHandler.removeCallbacks(any(Runnable::class.java))).thenAnswer(mockRemoveCallbacks)
    }

    @Test
    fun `eventsource connection is paused if app is backgrounded for more than 800ms`() {
        var onResumeCalled = false
        var onPauseCalled = false
        val dvcLifecycleCallbacks = DVCLifecycleCallbacks(fun () { onPauseCalled = true }, fun () { onResumeCalled = true }, null, mockHandler)

        dvcLifecycleCallbacks.onActivityPaused(mockActivity)
        countDownLatch.await(2000, TimeUnit.MILLISECONDS)

        Assertions.assertFalse(onResumeCalled)
        Assertions.assertTrue(onPauseCalled)
    }

    @Test
    fun `eventsource connection is not paused if app is backgrounded for less than 800ms`() {
        var onResumeCalled = false
        var onPauseCalled = false
        val dvcLifecycleCallbacks = DVCLifecycleCallbacks(fun () { onPauseCalled = true }, fun () { onResumeCalled = true }, null, mockHandler)

        dvcLifecycleCallbacks.onActivityPaused(mockActivity)
        dvcLifecycleCallbacks.onActivityResumed(mockActivity)
        countDownLatch.await(2000, TimeUnit.MILLISECONDS)

        verify(mockHandler, times(2)).removeCallbacks(any())
        Assertions.assertTrue(onResumeCalled)
        Assertions.assertFalse(onPauseCalled)
    }
}

private val mainThread: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
