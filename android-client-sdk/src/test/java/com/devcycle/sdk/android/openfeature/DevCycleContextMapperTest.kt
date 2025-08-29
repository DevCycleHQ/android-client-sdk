package com.devcycle.sdk.android.openfeature

import com.devcycle.sdk.android.util.JSONMapper
import com.fasterxml.jackson.core.type.TypeReference
import dev.openfeature.kotlin.sdk.ImmutableContext
import dev.openfeature.kotlin.sdk.Value
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DevCycleContextMapperTest {

    private fun convertToJsonMap(user: Any): Map<String, Any> {
        return JSONMapper.mapper.convertValue(user, object : TypeReference<Map<String, Any>>() {})
    }

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
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("user-123", jsonMap["userId"])
    }

    @Test
    fun `maps userId attribute to user ID when no targeting key`() {
        val context = ImmutableContext(
            attributes = mutableMapOf(
                "userId" to Value.String("user-from-userId")
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("user-from-userId", jsonMap["userId"])
        assertEquals(false, jsonMap["isAnonymous"]) // User should be identified
    }

    @Test
    fun `maps user_id attribute to user ID when no targeting key or userId`() {
        val context = ImmutableContext(
            attributes = mutableMapOf(
                "user_id" to Value.String("user-from-user_id")
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("user-from-user_id", jsonMap["userId"])
        assertEquals(false, jsonMap["isAnonymous"]) // User should be identified
    }

    @Test
    fun `prioritizes targeting key over userId and user_id attributes`() {
        val context = ImmutableContext(
            targetingKey = "targeting-key-user",
            attributes = mutableMapOf(
                "userId" to Value.String("userId-attribute"),
                "user_id" to Value.String("user_id-attribute")
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("targeting-key-user", jsonMap["userId"])
        
        // userId and user_id should not appear in custom data when targeting key is used
        val customData = jsonMap["customData"] as? Map<*, *>
        assertNull(customData?.get("userId"))
        assertNull(customData?.get("user_id"))
    }

    @Test
    fun `prioritizes user_id over userId attribute when no targeting key`() {
        val context = ImmutableContext(
            attributes = mutableMapOf(
                "userId" to Value.String("userId-attribute"),
                "user_id" to Value.String("user_id-attribute")
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result!!)
        // Should use user_id value as it has higher priority than userId (matching Java SDK)
        assertEquals("user_id-attribute", jsonMap["userId"])
        assertEquals(false, jsonMap["isAnonymous"]) // User should be identified
        
        // Neither should appear in custom data when used for user ID
        val customData = jsonMap["customData"] as? Map<*, *>
        assertNull(customData?.get("userId"))
        assertNull(customData?.get("user_id"))
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
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("user-123", jsonMap["userId"])
        assertEquals("test@example.com", jsonMap["email"])
        assertEquals("Test User", jsonMap["name"])
        assertEquals("US", jsonMap["country"])
        assertEquals(false, jsonMap["isAnonymous"])
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
        val jsonMap = convertToJsonMap(result!!)
        val customData = jsonMap["customData"] as Map<*, *>
        assertEquals("premium", customData["plan"])
        assertEquals("us-east-1", customData["region"])
        assertEquals(100, customData["score"])
        assertEquals(true, customData["isActive"])
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
        val jsonMap = convertToJsonMap(result!!)
        val privateCustomData = jsonMap["privateCustomData"] as Map<*, *>
        val customData = jsonMap["customData"] as Map<*, *>
        
        // Private data should be in privateCustomData without the prefix
        assertEquals("123-45-6789", privateCustomData["ssn"])
        assertEquals(9876, privateCustomData["internal_id"])
        
        // Public data should be in customData
        assertEquals("visible", customData["public_data"])
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
        val jsonMap = convertToJsonMap(result!!)
        val customData = jsonMap["customData"] as Map<*, *>
        
        // Check nested structure
        val preferences = customData["preferences"] as Map<*, *>
        assertEquals("dark", preferences["theme"])
        assertEquals(true, preferences["notifications"])
        
        val limits = preferences["limits"] as Map<*, *>
        assertEquals(100, limits["daily"])
        assertEquals(3000, limits["monthly"])
        
        // Check list
        val tags = customData["tags"] as List<*>
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
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("user-123", jsonMap["userId"])
        assertNull(jsonMap["email"]) // Should be null because it wasn't a string
        assertNull(jsonMap["name"]) // Should be null because it wasn't a string
        assertEquals("US", jsonMap["country"]) // Should be set because it was a string
        assertEquals(false, jsonMap["isAnonymous"]) // Should be false because user has targetingKey (identified user)
        
        // Wrong-type standard attributes should appear in custom data
        val customData = jsonMap["customData"] as Map<*, *>
        assertEquals(123, customData["email"])
        assertEquals(true, customData["name"])
        assertEquals("false", customData["isAnonymous"])
    }

    @Test
    fun `handles empty context gracefully`() {
        val context = ImmutableContext()
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(context)
        
        // Empty context should return null since there's no meaningful data
        assertNull(result)
    }

    @Test
    fun `maps context with nested customData structure`() {
        val evaluationContext = ImmutableContext(
            targetingKey = "test_user",
            attributes = mutableMapOf(
                "email" to Value.String("test@devcycle.com"),
                "name" to Value.String("Test User"),
                "country" to Value.String("CA"),
                "customData" to Value.Structure(mapOf(
                    "custom_value" to Value.String("test")
                ))
            )
        )
        
        val result = DevCycleContextMapper.evaluationContextToDevCycleUser(evaluationContext)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result!!)
        assertEquals("test_user", jsonMap["userId"])
        assertEquals("test@devcycle.com", jsonMap["email"])
        assertEquals("Test User", jsonMap["name"])
        assertEquals("CA", jsonMap["country"])
        assertEquals(false, jsonMap["isAnonymous"])
        
        // Check that the nested customData structure is properly mapped
        val customData = jsonMap["customData"] as Map<*, *>
        assertEquals("test", customData["custom_value"])
    }
} 