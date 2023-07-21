package com.devcycle.sdk.android.helpers

import com.devcycle.sdk.android.util.DevCycleLogger

/**
 * Collect logs for Test Assertions
 */
class TestDVCLogger : DevCycleLogger.Logger() {
    val logs = mutableListOf<Pair<Int, String>>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logs.add(Pair(priority, message))
    }
}