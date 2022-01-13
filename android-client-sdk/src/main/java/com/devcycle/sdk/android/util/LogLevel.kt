package com.devcycle.sdk.android.util

import android.util.Log

enum class LogLevel(val value: Int) {
    VERBOSE(Log.VERBOSE),
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR),
    NO_LOGGING(0)
}