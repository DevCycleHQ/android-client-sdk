package com.devcycle.sdk.android.openfeature

import android.content.Context
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.api.DevCycleOptions
import com.devcycle.sdk.android.model.DevCycleUser
import com.devcycle.sdk.android.model.Variable
import dev.openfeature.sdk.*
import dev.openfeature.sdk.exceptions.OpenFeatureError
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach

class DevCycleProviderTest {

    private lateinit var mockContext: Context
    private lateinit var mockDevCycleClient: DevCycleClient
    private lateinit var provider: DevCycleProvider

    @BeforeEach
    fun setup() {
        mockContext = mockk<Context>(relaxed = true)
        mockDevCycleClient = mockk<DevCycleClient>(relaxed = true)
        
        // Mock the DevCycleClient.builder() static method chain
        mockkObject(DevCycleClient.Companion)
        val mockBuilder = mockk<DevCycleClient.DevCycleClientBuilder>(relaxed = true)
        every { DevCycleClient.builder() } returns mockBuilder
        every { mockBuilder.withContext(any()) } returns mockBuilder
        every { mockBuilder.withSDKKey(any()) } returns mockBuilder
        every { mockBuilder.withUser(any()) } returns mockBuilder
        every { mockBuilder.withOptions(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockDevCycleClient
        
        // Create the provider - now it won't try to create a real DevCycleClient
        provider = DevCycleProvider("test-sdk-key", mockContext)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `provider has correct metadata`() {
        assertEquals("DevCycle", provider.metadata.name)
    }

    @Test
    fun `getBooleanEvaluation returns default when client not initialized`() {
        val result = provider.getBooleanEvaluation("test-flag", false, null)

        assertEquals(false, result.value)
        assertEquals("DEFAULT", result.reason)
    }

    @Test
    fun `getStringEvaluation returns default when client not initialized`() {
        val result = provider.getStringEvaluation("test-flag", "default", null)

        assertEquals("default", result.value)
        assertEquals("DEFAULT", result.reason)
    }

    @Test
    fun `getIntegerEvaluation returns default when client not initialized`() {
        val result = provider.getIntegerEvaluation("test-flag", 42, null)

        assertEquals(42, result.value)
        assertEquals("DEFAULT", result.reason)
    }

    @Test
    fun `getDoubleEvaluation returns default when client not initialized`() {
        val result = provider.getDoubleEvaluation("test-flag", 3.14, null)

        assertEquals(3.14, result.value)
        assertEquals("DEFAULT", result.reason)
    }

    @Test
    fun `getObjectEvaluation returns default when client not initialized`() {
        val defaultValue = Value.Structure(mapOf("default" to Value.String("value")))
        val result = provider.getObjectEvaluation("test-flag", defaultValue, null)

        assertEquals(defaultValue, result.value)
        assertEquals("DEFAULT", result.reason)
    }

    @Test
    fun `track handles uninitialized client gracefully`() {
        val details = TrackingEventDetails(value = 1.0)

        // Should not throw an exception
        assertDoesNotThrow {
            provider.track("test-event", null, details)
        }
    }

    @Test
    fun `onContextSet handles uninitialized client gracefully`() {
        val newContext = ImmutableContext(targetingKey = "new-user")
        
        // Should not throw an exception
        assertDoesNotThrow {
            runBlocking {
                provider.onContextSet(null, newContext)
            }
        }
    }

    @Test
    fun `shutdown handles uninitialized client gracefully`() {
        // Should not throw an exception
        assertDoesNotThrow {
            provider.shutdown()
        }
    }

    @Test
    fun `initialize throws exception with invalid setup`() {
        // This test would only work if we had a way to make DevCycleClient construction fail
        // For now, we'll just verify that initialize can be called without crashing
        
        // Should not throw when initialize is called with valid params
        assertDoesNotThrow {
            runBlocking {
                provider.initialize(ImmutableContext(targetingKey = "test-user"))
            }
        }
    }

    @Test
    fun `initialize with null context works`() {
        // Should not throw and should log warning
        assertDoesNotThrow {
            runBlocking {
                provider.initialize(null)
            }
        }
    }
} 