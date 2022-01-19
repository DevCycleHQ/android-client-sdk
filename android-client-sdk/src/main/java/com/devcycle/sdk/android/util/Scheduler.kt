package com.devcycle.sdk.android.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Scheduler(private val coroutineScope: CoroutineScope, private val delayMillis: Long) {
    private val isTimerRunning = AtomicBoolean(false)

    fun scheduleWithDelay(action: () -> Unit) = coroutineScope.launch {
        if (isTimerRunning.get()) {
            return@launch
        }
        isTimerRunning.set(true)
        delay(delayMillis)
        action()
        isTimerRunning.set(false)
    }
}