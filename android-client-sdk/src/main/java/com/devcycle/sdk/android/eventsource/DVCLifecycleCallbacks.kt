package com.devcycle.sdk.android.eventsource

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import timber.log.Timber

class DVCLifecycleCallbacks(eventSource: EventSource) : Application.ActivityLifecycleCallbacks {
    private val CHECK_DELAY: Long = 800

    private val eventSource = eventSource
    private val listenerThread: HandlerThread = HandlerThread("DVCForegroundListener")
    private var handler: Handler? = null
    private var paused = false
    private var foreground = true
    private var check: Runnable? = null

    init {
        listenerThread.start()
        handler = Handler(listenerThread.looper)
    }
    override fun onActivityResumed(activity: Activity) {
        paused = false
        if (check != null) {
            handler!!.removeCallbacks(check!!) // stop check from running if the timer is still running
            check = null
        }
        if (!foreground) {
            Timber.d("Restarting connection")
            eventSource.start()
        }
        foreground = true
    }

    override fun onActivityPaused(activity: Activity) {
        paused = true

        if (check != null) {
            handler!!.removeCallbacks(check!!)
            check = null
        }

        check = Runnable {
            if (foreground && paused) {
                foreground = false
                Timber.d("App backgrounded for $CHECK_DELAY ms, closing connection")
                eventSource.close()
            } else {
                Timber.d("App paused but still foreground")
            }
        }

        handler!!.postDelayed(check!!, CHECK_DELAY)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}
