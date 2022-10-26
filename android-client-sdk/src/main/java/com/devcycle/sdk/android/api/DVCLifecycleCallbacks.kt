package com.devcycle.sdk.android.api

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread

class DVCLifecycleCallbacks(
    onPause: () -> Any,
    onResume: () -> Any,
    inactivityDelay: Long? = null,
    customHandler: Handler? = null
) : Application.ActivityLifecycleCallbacks {
    private val inactivityDelay: Long = inactivityDelay ?: 800

    private val onResume = onResume
    private val onPause = onPause
    private val listenerThread: HandlerThread = HandlerThread("DVCForegroundListener")
    private val delayHandler: Handler
    private val closeConnectionCallback = Runnable {
        onPause()
    }

    init {
        if (customHandler == null) listenerThread.start()
        delayHandler = customHandler ?: Handler(listenerThread.looper)
    }
    override fun onActivityResumed(activity: Activity) {
        delayHandler.removeCallbacks(closeConnectionCallback) // stop check from running if the timer is still running
        onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        delayHandler.removeCallbacks(closeConnectionCallback)
        delayHandler.postDelayed(closeConnectionCallback, inactivityDelay)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}
