package com.devcycle.sdk.android.helpers

import timber.log.Timber
import java.time.LocalDateTime

/**
 * Collect logs for Test Assertions
 */
class TestTree : Timber.Tree() {
    val logs = mutableListOf<Log>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logs.add(Log(LocalDateTime.now(), priority, tag, message, t))
    }

    data class Log(
        val now: LocalDateTime,
        val priority: Int,
        val tag: String?,
        val message: String,
        val t: Throwable?)
}
