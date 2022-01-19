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
            Log.e(tag, message, t)
            return
        }

        Log.println(priority, tag, message)
    }
}