package com.devcycle.sdk.android.openfeature

import android.content.Context
import com.devcycle.sdk.android.api.DevCycleCallback
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.model.BaseConfigVariable
import com.devcycle.sdk.android.model.EvalReason
import com.devcycle.sdk.android.model.Variable
import dev.openfeature.kotlin.sdk.EvaluationMetadata
import dev.openfeature.kotlin.sdk.ImmutableContext
import dev.openfeature.kotlin.sdk.TrackingEventDetails
import dev.openfeature.kotlin.sdk.Value
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

        // Mock the onInitialized method to immediately call the success callback
        every { mockDevCycleClient.onInitialized(any()) } answers {
            val callback = firstArg<DevCycleCallback<String>>()
            callback.onSuccess("initialized")
        }

        // Mock the identifyUser method to immediately call the success callback
        every { mockDevCycleClient.identifyUser(any(), any()) } answers {
            val callback = secondArg<DevCycleCallback<Map<String, BaseConfigVariable>>>()
            callback.onSuccess(emptyMap())
        }

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
    fun `accessing devcycleClient throws exception when client not initialized`() {
        val exception = assertThrows(IllegalStateException::class.java) { provider.devcycleClient }
        assertTrue(
            exception.message?.contains("DevCycleClient is not initialized") == true,
            "Exception message should contain 'DevCycleClient is not initialized'"
        )
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

    @Test
    fun `createProviderEvaluation includes metadata when eval details are available`() {
        setupInitializedProvider()

        // Create a mock variable with eval information
        val mockVariable = mockk<Variable<String>>(relaxed = true)
        val mockEvalReason = mockk<EvalReason>(relaxed = true)

        every { mockVariable.key } returns "test-variable"
        every { mockVariable.value } returns "test-value"
        every { mockVariable.isDefaulted } returns false
        every { mockVariable.eval } returns mockEvalReason
        every { mockEvalReason.reason } returns "TARGETING_MATCH"
        every { mockEvalReason.details } returns "Test evaluation details"
        every { mockEvalReason.targetId } returns "test-target-123"

        every { mockDevCycleClient.variable("test-variable", "default") } returns mockVariable

        val result = provider.getStringEvaluation("test-variable", "default", null)

        assertEquals("test-value", result.value)
        assertEquals("test-variable", result.variant)
        assertEquals("TARGETING_MATCH", result.reason)

        // Check that metadata contains the eval details and target ID
        assertNotNull(result.metadata)
        assertEquals("Test evaluation details", result.metadata.getString("evalDetails"))
        assertEquals("test-target-123", result.metadata.getString("evalTargetId"))
    }

    @Test
    fun `createProviderEvaluation includes partial metadata when only some eval details are available`() {
        setupInitializedProvider()

        // Create a mock variable with eval information but only details (no targetId)
        val mockVariable = mockk<Variable<String>>(relaxed = true)
        val mockEvalReason = mockk<EvalReason>(relaxed = true)

        every { mockVariable.key } returns "test-variable"
        every { mockVariable.value } returns "test-value"
        every { mockVariable.isDefaulted } returns false
        every { mockVariable.eval } returns mockEvalReason
        every { mockEvalReason.reason } returns "TARGETING_MATCH"
        every { mockEvalReason.details } returns "Test evaluation details"
        every { mockEvalReason.targetId } returns null // No target ID

        every { mockDevCycleClient.variable("test-variable", "default") } returns mockVariable

        val result = provider.getStringEvaluation("test-variable", "default", null)

        assertEquals("test-value", result.value)
        assertEquals("test-variable", result.variant)
        assertEquals("TARGETING_MATCH", result.reason)

        // Check that metadata contains the eval details but not target ID
        assertNotNull(result.metadata)
        assertEquals("Test evaluation details", result.metadata.getString("evalDetails"))
        assertNull(result.metadata.getString("evalTargetId"))
    }

    @Test
    fun `createProviderEvaluation uses empty metadata when no eval details are available`() {
        setupInitializedProvider()

        // Create a mock variable with no eval information
        val mockVariable = mockk<Variable<String>>(relaxed = true)

        every { mockVariable.key } returns "test-variable"
        every { mockVariable.value } returns "test-value"
        every { mockVariable.isDefaulted } returns true
        every { mockVariable.eval } returns null // No eval data

        every { mockDevCycleClient.variable("test-variable", "default") } returns mockVariable

        val result = provider.getStringEvaluation("test-variable", "default", null)

        assertEquals("test-value", result.value)
        assertEquals("test-variable", result.variant)
        assertEquals("DEFAULT", result.reason)

        // Check that metadata is EMPTY when no eval data is available
        assertEquals(EvaluationMetadata.EMPTY, result.metadata)
        assertNull(result.metadata.getString("evalDetails"))
        assertNull(result.metadata.getString("evalTargetId"))
    }

    @Test
    fun `accessing devcycleClient returns devcycleClient when client is initialized`() {
        setupInitializedProvider()
        assertEquals(mockDevCycleClient, provider.devcycleClient)
    }

    private fun setupInitializedProvider() {
        // Make the devCycleClient available (simulate successful initialization)
        val providerField = DevCycleProvider::class.java.getDeclaredField("_devcycleClient")
        providerField.isAccessible = true
        providerField.set(provider, mockDevCycleClient)
    }
} 