package com.devcycle.sdk.android.helpers

import com.devcycle.sdk.android.util.DVCLogger

/**
 * Collect logs for Test Assertions
 */
class TestDVCLogger : DVCLogger.Logger() {
    val logs = mutableListOf<Pair<Int, String>>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logs.add(Pair(priority, message))
    }
}