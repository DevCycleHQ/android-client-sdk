package com.devcycle.sdk.android.openfeature

import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Value
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DevCycleContextMapperTest {

    @Test
    fun `returns null for null context`() {
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(null)
        assertNull(result)
    }

    @Test
    fun `maps targeting key to user ID`() {
        val context = ImmutableContext(targetingKey = "user-123")
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        assertEquals("user-123", result!!.userId)
    }

    @Test
    fun `maps standard attributes correctly`() {
        val context = ImmutableContext(
            targetingKey = "user-123",
            attributes = mutableMapOf(
                "email" to Value.String("test@example.com"),
                "name" to Value.String("Test User"),
                "country" to Value.String("US"),
                "isAnonymous" to Value.Boolean(false)
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        assertEquals("user-123", result!!.userId)
        assertEquals("test@example.com", result.email)
        assertEquals("Test User", result.name)
        assertEquals("US", result.country)
        assertEquals(false, result.isAnonymous)
    }

    @Test
    fun `maps custom data correctly`() {
        val context = ImmutableContext(
            targetingKey = "user-123",
            attributes = mutableMapOf(
                "plan" to Value.String("premium"),
                "region" to Value.String("us-east-1"),
                "score" to Value.Integer(100),
                "isActive" to Value.Boolean(true)
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        assertNotNull(result!!.customData)
        assertEquals("premium", result.customData!!["plan"])
        assertEquals("us-east-1", result.customData!!["region"])
        assertEquals(100, result.customData!!["score"])
        assertEquals(true, result.customData!!["isActive"])
    }

    @Test
    fun `maps private custom data correctly`() {
        val context = ImmutableContext(
            targetingKey = "user-123",
            attributes = mutableMapOf(
                "private_ssn" to Value.String("123-45-6789"),
                "private_internal_id" to Value.Integer(9876),
                "public_data" to Value.String("visible")
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        assertNotNull(result!!.privateCustomData)
        assertNotNull(result.customData)
        
        // Private data should be in privateCustomData without the prefix
        assertEquals("123-45-6789", result.privateCustomData!!["ssn"])
        assertEquals(9876, result.privateCustomData!!["internal_id"])
        
        // Public data should be in customData
        assertEquals("visible", result.customData!!["public_data"])
    }

    @Test
    fun `handles complex nested values`() {
        val context = ImmutableContext(
            targetingKey = "user-123",
            attributes = mutableMapOf(
                "preferences" to Value.Structure(mapOf(
                    "theme" to Value.String("dark"),
                    "notifications" to Value.Boolean(true),
                    "limits" to Value.Structure(mapOf(
                        "daily" to Value.Integer(100),
                        "monthly" to Value.Integer(3000)
                    ))
                )),
                "tags" to Value.List(listOf(
                    Value.String("premium"),
                    Value.String("beta-user")
                ))
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        assertNotNull(result!!.customData)
        
        // Check nested structure
        val preferences = result.customData!!["preferences"] as Map<*, *>
        assertEquals("dark", preferences["theme"])
        assertEquals(true, preferences["notifications"])
        
        val limits = preferences["limits"] as Map<*, *>
        assertEquals(100, limits["daily"])
        assertEquals(3000, limits["monthly"])
        
        // Check list
        val tags = result.customData!!["tags"] as List<*>
        assertEquals(2, tags.size)
        assertEquals("premium", tags[0])
        assertEquals("beta-user", tags[1])
    }

    @Test
    fun `ignores non-string standard attributes`() {
        val context = ImmutableContext(
            targetingKey = "user-123",
            attributes = mutableMapOf(
                "email" to Value.Integer(123), // Should be ignored for email field but go to custom data
                "name" to Value.Boolean(true), // Should be ignored for name field but go to custom data
                "country" to Value.String("US"), // Should be kept
                "isAnonymous" to Value.String("false") // Should be ignored for isAnonymous field but go to custom data
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        assertEquals("user-123", result!!.userId)
        assertNull(result.email) // Should be null because it wasn't a string
        assertNull(result.name) // Should be null because it wasn't a string
        assertEquals("US", result.country) // Should be set because it was a string
        assertEquals(false, result.isAnonymous) // Should be false because user has targetingKey (identified user)
        
        // Wrong-type standard attributes should appear in custom data
        assertNotNull(result.customData)
        assertEquals(123, result.customData!!["email"])
        assertEquals(true, result.customData!!["name"])
        assertEquals("false", result.customData!!["isAnonymous"])
    }

    @Test
    fun `handles empty context gracefully`() {
        val context = ImmutableContext()
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        // Empty context should return null since there's no meaningful data
        assertNull(result)
    }
} 