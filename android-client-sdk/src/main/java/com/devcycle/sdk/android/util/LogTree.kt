package com.devcycle.sdk.android.util

import android.util.Log
import timber.log.Timber

/**
 * Implementation of Timber Tree which adds configurable log level
 */
class LogTree(private val logLevel: Int) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < logLevel) {
            return
        }

        if (t != null) {
            Timber.e(t, message)
            return
        }

        Log.println(priority, tag, message)
    }
}