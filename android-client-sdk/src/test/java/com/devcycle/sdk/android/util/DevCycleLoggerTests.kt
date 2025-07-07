package com.devcycle.sdk.android.util

import android.util.Log
import com.devcycle.sdk.android.helpers.TestDVCLogger
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class DevCycleLoggerTests {

    private val logger = TestDVCLogger()

    @BeforeEach
    fun setUp() {
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
        try {
            DevCycleLogger.stop(logger)
        } catch (e: IllegalArgumentException) {
            // Logger wasn't started, which is fine
        }
        logger.logs.clear()
    }

    @AfterEach
    fun tearDown() {
        try {
            DevCycleLogger.stop(logger)
        } catch (e: IllegalArgumentException) {
            // Logger wasn't started, which is fine
        }
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
    }

    @Test
    fun `test log level filtering works correctly`() {
        DevCycleLogger.start(logger)

        // Test DEBUG level - should log DEBUG and above
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.e("Error message")

        assertEquals(2, logger.logs.size) // Should see DEBUG and ERROR, not VERBOSE
        assertEquals(Log.DEBUG, logger.logs[0].first)
        assertEquals(Log.ERROR, logger.logs[1].first)

        // Clear and test ERROR level - should only log ERROR
        logger.logs.clear()
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
        DevCycleLogger.d("Debug message")
        DevCycleLogger.e("Error message")

        assertEquals(1, logger.logs.size) // Should only see ERROR
        assertEquals(Log.ERROR, logger.logs[0].first)
    }

    @Test
    fun `test NO_LOGGING disables all logging`() {
        DevCycleLogger.setMinLogLevel(LogLevel.NO_LOGGING)
        DevCycleLogger.start(logger)

        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        assertEquals(0, logger.logs.size) // Should log nothing
    }

    @Test
    fun `test setMinLogLevel updates the log level`() {
        assertEquals(LogLevel.ERROR, DevCycleLogger.minLogLevel)
        
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        assertEquals(LogLevel.DEBUG, DevCycleLogger.minLogLevel)
        
        DevCycleLogger.setMinLogLevel(LogLevel.NO_LOGGING)
        assertEquals(LogLevel.NO_LOGGING, DevCycleLogger.minLogLevel)
    }
}