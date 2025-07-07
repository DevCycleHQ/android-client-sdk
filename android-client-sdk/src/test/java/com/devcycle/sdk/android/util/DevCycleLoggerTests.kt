package com.devcycle.sdk.android.util

import android.util.Log
import com.devcycle.sdk.android.helpers.TestDVCLogger
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class DevCycleLoggerTests {

    private val logger = TestDVCLogger()

    @BeforeEach
    fun setUp() {
        // Reset DevCycleLogger state before each test
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
        // Clean up after each test
        try {
            DevCycleLogger.stop(logger)
        } catch (e: IllegalArgumentException) {
            // Logger wasn't started, which is fine
        }
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
    }

    @Test
    fun `test log level filtering - VERBOSE logs everything`() {
        DevCycleLogger.setMinLogLevel(LogLevel.VERBOSE)
        DevCycleLogger.start(logger)

        // Log messages at all levels
        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        // All messages should be logged
        assertEquals(5, logger.logs.size)
        
        val logLevels = logger.logs.map { it.first }
        assertTrue(logLevels.contains(Log.VERBOSE))
        assertTrue(logLevels.contains(Log.DEBUG))
        assertTrue(logLevels.contains(Log.INFO))
        assertTrue(logLevels.contains(Log.WARN))
        assertTrue(logLevels.contains(Log.ERROR))
    }

    @Test
    fun `test log level filtering - DEBUG filters out VERBOSE`() {
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        DevCycleLogger.start(logger)

        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        // Should log DEBUG and above (4 messages)
        assertEquals(4, logger.logs.size)
        
        val logLevels = logger.logs.map { it.first }
        assertFalse(logLevels.contains(Log.VERBOSE))
        assertTrue(logLevels.contains(Log.DEBUG))
        assertTrue(logLevels.contains(Log.INFO))
        assertTrue(logLevels.contains(Log.WARN))
        assertTrue(logLevels.contains(Log.ERROR))
    }

    @Test
    fun `test log level filtering - INFO filters out VERBOSE and DEBUG`() {
        DevCycleLogger.setMinLogLevel(LogLevel.INFO)
        DevCycleLogger.start(logger)

        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        // Should log INFO and above (3 messages)
        assertEquals(3, logger.logs.size)
        
        val logLevels = logger.logs.map { it.first }
        assertFalse(logLevels.contains(Log.VERBOSE))
        assertFalse(logLevels.contains(Log.DEBUG))
        assertTrue(logLevels.contains(Log.INFO))
        assertTrue(logLevels.contains(Log.WARN))
        assertTrue(logLevels.contains(Log.ERROR))
    }

    @Test
    fun `test log level filtering - WARN filters out VERBOSE, DEBUG, and INFO`() {
        DevCycleLogger.setMinLogLevel(LogLevel.WARN)
        DevCycleLogger.start(logger)

        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        // Should log WARN and above (2 messages)
        assertEquals(2, logger.logs.size)
        
        val logLevels = logger.logs.map { it.first }
        assertFalse(logLevels.contains(Log.VERBOSE))
        assertFalse(logLevels.contains(Log.DEBUG))
        assertFalse(logLevels.contains(Log.INFO))
        assertTrue(logLevels.contains(Log.WARN))
        assertTrue(logLevels.contains(Log.ERROR))
    }

    @Test
    fun `test log level filtering - ERROR filters out everything except ERROR`() {
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
        DevCycleLogger.start(logger)

        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        // Should log ERROR only (1 message)
        assertEquals(1, logger.logs.size)
        
        val logLevels = logger.logs.map { it.first }
        assertFalse(logLevels.contains(Log.VERBOSE))
        assertFalse(logLevels.contains(Log.DEBUG))
        assertFalse(logLevels.contains(Log.INFO))
        assertFalse(logLevels.contains(Log.WARN))
        assertTrue(logLevels.contains(Log.ERROR))
    }

    @Test
    fun `test log level filtering - NO_LOGGING filters out everything`() {
        DevCycleLogger.setMinLogLevel(LogLevel.NO_LOGGING)
        DevCycleLogger.start(logger)

        DevCycleLogger.v("Verbose message")
        DevCycleLogger.d("Debug message")
        DevCycleLogger.i("Info message")
        DevCycleLogger.w("Warning message")
        DevCycleLogger.e("Error message")

        // Should log nothing
        assertEquals(0, logger.logs.size)
    }

    @Test
    fun `test log level changes affect subsequent logging`() {
        DevCycleLogger.start(logger)
        
        // Start with ERROR level
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
        DevCycleLogger.d("Debug message 1")
        DevCycleLogger.e("Error message 1")
        
        // Should only see error
        assertEquals(1, logger.logs.size)
        assertEquals(Log.ERROR, logger.logs[0].first)
        
        // Change to DEBUG level
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        DevCycleLogger.d("Debug message 2")
        DevCycleLogger.e("Error message 2")
        
        // Should now see both debug and error (total 3)
        assertEquals(3, logger.logs.size)
        val newLogs = logger.logs.drop(1) // Skip the first error message
        assertEquals(Log.DEBUG, newLogs[0].first)
        assertEquals(Log.ERROR, newLogs[1].first)
    }

    @Test
    fun `test log message formatting with arguments`() {
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        DevCycleLogger.start(logger)

        DevCycleLogger.d("Debug message with %s and %d", "string", 42)
        DevCycleLogger.i("Info message with %s", "parameter")

        assertEquals(2, logger.logs.size)
        assertEquals("Debug message with string and 42", logger.logs[0].second)
        assertEquals("Info message with parameter", logger.logs[1].second)
    }

    @Test
    fun `test log message with exception`() {
        DevCycleLogger.setMinLogLevel(LogLevel.ERROR)
        DevCycleLogger.start(logger)

        val exception = RuntimeException("Test exception")
        DevCycleLogger.e(exception, "Error occurred: %s", "test scenario")

        assertEquals(1, logger.logs.size)
        val logMessage = logger.logs[0].second
        assertTrue(logMessage.contains("Error occurred: test scenario"))
        assertTrue(logMessage.contains("RuntimeException"))
        assertTrue(logMessage.contains("Test exception"))
    }

    @Test
    fun `test setMinLogLevel function updates the minimum log level`() {
        // Test initial state
        assertEquals(LogLevel.ERROR, DevCycleLogger.minLogLevel)
        
        // Test changing to DEBUG
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        assertEquals(LogLevel.DEBUG, DevCycleLogger.minLogLevel)
        
        // Test changing to INFO
        DevCycleLogger.setMinLogLevel(LogLevel.INFO)
        assertEquals(LogLevel.INFO, DevCycleLogger.minLogLevel)
        
        // Test changing to NO_LOGGING
        DevCycleLogger.setMinLogLevel(LogLevel.NO_LOGGING)
        assertEquals(LogLevel.NO_LOGGING, DevCycleLogger.minLogLevel)
    }

    @Test
    fun `test logger start and stop functionality`() {
        // Initially logger should not be started
        DevCycleLogger.setMinLogLevel(LogLevel.DEBUG)
        DevCycleLogger.d("This should not be logged")
        assertEquals(0, logger.logs.size)
        
        // Start logger
        DevCycleLogger.start(logger)
        DevCycleLogger.d("This should be logged")
        assertEquals(1, logger.logs.size)
        
        // Stop logger
        DevCycleLogger.stop(logger)
        DevCycleLogger.d("This should not be logged again")
        assertEquals(1, logger.logs.size) // Should still be 1
    }

    @Test
    fun `test LogLevel enum values correspond to Android Log constants`() {
        assertEquals(Log.VERBOSE, LogLevel.VERBOSE.value)
        assertEquals(Log.DEBUG, LogLevel.DEBUG.value)
        assertEquals(Log.INFO, LogLevel.INFO.value)
        assertEquals(Log.WARN, LogLevel.WARN.value)
        assertEquals(Log.ERROR, LogLevel.ERROR.value)
        assertEquals(0, LogLevel.NO_LOGGING.value)
    }
}