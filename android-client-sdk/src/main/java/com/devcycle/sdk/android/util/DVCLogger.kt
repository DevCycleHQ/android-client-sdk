package com.devcycle.sdk.android.util

import android.util.Log

open class DVCLogger(private val logLevel: LogLevel) {
    companion object {
        private var instance: DVCLogger? = null
        var TAG: String = "DVC"

        @JvmOverloads
        fun getInstance(logLevel: LogLevel ?= LogLevel.ERROR): DVCLogger {
            if (instance == null) {
                instance = logLevel?.let { DVCLogger(it) }
            }
            return instance!!
        }
    }

    open fun log(priority: LogLevel, message: String, t: Throwable?=null): DVCLogger {
        if (priority.value < logLevel.value) {
            return this
        }

        if (t != null) {
            Log.e(TAG, message, t)
            return this
        }

        Log.println(priority.value, TAG, message)
        return this
    }

    open fun v(message: String) {
        this.log(LogLevel.VERBOSE, message)
    }

    open fun d(message: String) {
        this.log(LogLevel.DEBUG, message)
    }

    open fun i(message: String) {
        this.log(LogLevel.INFO, message)
    }

    @JvmOverloads
    open fun w(message: String, t: Throwable?=null) {
        this.log(LogLevel.WARN, message, t)
    }

    @JvmOverloads
    open fun e(message: String, t: Throwable?=null) {
        this.log(LogLevel.ERROR, message, t)
    }
}