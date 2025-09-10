package com.devcycle.sdk.android.openfeature

import com.devcycle.sdk.android.util.JSONMapper
import com.fasterxml.jackson.core.type.TypeReference
import dev.openfeature.kotlin.sdk.ImmutableStructure
import dev.openfeature.kotlin.sdk.TrackingEventDetails
import dev.openfeature.kotlin.sdk.Value
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class DevCycleEventMapperTest {

    private fun convertToJsonMap(event: Any): Map<String, Any> {
        return JSONMapper.mapper.convertValue(event, object : TypeReference<Map<String, Any>>() {})
    }

    @Test
    fun `creates event with only event name when no details provided`() {
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("test_event", null)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("test_event", jsonMap["type"])
        assertNull(jsonMap["value"])
        assertNull(jsonMap["metaData"])
    }

    @Test
    fun `creates event with event name and empty details`() {
        val details = TrackingEventDetails()
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("test_event", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("test_event", jsonMap["type"])
        assertNull(jsonMap["value"])
        assertNull(jsonMap["metaData"])
    }

    @Test
    fun `processes numeric value correctly - integer`() {
        val details = TrackingEventDetails(value = 42)
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("purchase", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("purchase", jsonMap["type"])
        assertEquals(BigDecimal.valueOf(42.0), jsonMap["value"])
    }

    @Test
    fun `processes numeric value correctly - double`() {
        val details = TrackingEventDetails(value = 99.99)
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("purchase", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("purchase", jsonMap["type"])
        assertEquals(BigDecimal.valueOf(99.99), jsonMap["value"])
    }

    @Test
    fun `processes numeric value correctly - float`() {
        val details = TrackingEventDetails(value = 123.45f)
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("score", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("score", jsonMap["type"])
        assertEquals(BigDecimal.valueOf(123.45f.toDouble()), jsonMap["value"]) // Float precision
    }

    @Test
    fun `processes BigDecimal value correctly`() {
        val details = TrackingEventDetails(value = java.math.BigDecimal("999.99"))
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("purchase", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("purchase", jsonMap["type"])
        assertEquals(BigDecimal("999.99"), jsonMap["value"])
    }

    @Test
    fun `processes simple metadata correctly`() {
        val structure = ImmutableStructure(mapOf(
            "product_id" to Value.String("abc123"),
            "quantity" to Value.Integer(2),
            "premium" to Value.Boolean(true)
        ))
        
        val details = TrackingEventDetails(structure = structure)
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("purchase", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("purchase", jsonMap["type"])
        
        val metaData = jsonMap["metaData"] as Map<*, *>
        assertEquals("abc123", metaData["product_id"])
        assertEquals(2, metaData["quantity"])
        assertEquals(true, metaData["premium"])
    }

    @Test
    fun `processes event with both value and metadata`() {
        val structure = ImmutableStructure(mapOf(
            "product_id" to Value.String("laptop_123"),
            "category" to Value.String("electronics"),
            "discount_applied" to Value.Boolean(true)
        ))
        
        val details = TrackingEventDetails(
            value = 1299.99,
            structure = structure
        )
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("purchase_completed", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("purchase_completed", jsonMap["type"])
        assertEquals(BigDecimal.valueOf(1299.99), jsonMap["value"])
        
        val metaData = jsonMap["metaData"] as Map<*, *>
        assertEquals("laptop_123", metaData["product_id"])
        assertEquals("electronics", metaData["category"])
        assertEquals(true, metaData["discount_applied"])
    }

    @Test
    fun `processes complex nested metadata structures correctly`() {
        val structure = ImmutableStructure(mapOf(
            "user" to Value.Structure(mapOf(
                "name" to Value.String("John Doe"),
                "age" to Value.Integer(25),
                "preferences" to Value.Structure(mapOf(
                    "theme" to Value.String("dark"),
                    "notifications" to Value.Boolean(true)
                ))
            )),
            "tags" to Value.List(listOf(
                Value.String("premium"),
                Value.String("beta-user"),
                Value.Integer(42)
            )),
            "scores" to Value.List(listOf(
                Value.Double(95.5),
                Value.Double(87.2)
            )),
            "product_id" to Value.String("abc123")
        ))
        
        val details = TrackingEventDetails(structure = structure)
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("complex_event", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("complex_event", jsonMap["type"])
        
        val metaData = jsonMap["metaData"] as Map<*, *>
        
        // Check nested user object
        val user = metaData["user"] as Map<*, *>
        assertEquals("John Doe", user["name"])
        assertEquals(25, user["age"])
        
        val preferences = user["preferences"] as Map<*, *>
        assertEquals("dark", preferences["theme"])
        assertEquals(true, preferences["notifications"])
        
        // Check lists with mixed types
        val tags = metaData["tags"] as List<*>
        assertEquals(3, tags.size)
        assertEquals("premium", tags[0])
        assertEquals("beta-user", tags[1])
        assertEquals(42, tags[2])
        
        val scores = metaData["scores"] as List<*>
        assertEquals(2, scores.size)
        assertEquals(95.5, scores[0])
        assertEquals(87.2, scores[1])
        
        // Check primitive value
        assertEquals("abc123", metaData["product_id"])
    }

    @Test
    fun `handles empty structure by not including metadata`() {
        val structure = ImmutableStructure(emptyMap<String, Value>())
        
        val details = TrackingEventDetails(structure = structure)
        
        val result = DevCycleEventMapper.openFeatureEventToDevCycleEvent("test_event", details)
        
        assertNotNull(result)
        val jsonMap = convertToJsonMap(result)
        assertEquals("test_event", jsonMap["type"])
        assertNull(jsonMap["metaData"])
    }
} 