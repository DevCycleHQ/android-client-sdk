package com.devcycle.sdk.android.helpers

import com.devcycle.sdk.android.util.DVCLogger
import com.devcycle.sdk.android.util.LogLevel
import java.time.LocalDateTime

/**
 * Collect logs for Test Assertions
 */
class TestDVCLogger : DVCLogger(LogLevel.VERBOSE) {
    val logs = mutableListOf<Pair<Int, String>>()

    override fun log(priority: LogLevel, message: String, t: Throwable?): DVCLogger {
        logs.add(Pair(priority.value, message))
        return this
    }
}
