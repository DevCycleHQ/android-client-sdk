package com.devcycle.sdk.android.openfeature

import com.devcycle.sdk.android.util.JSONMapper
import com.fasterxml.jackson.core.type.TypeReference
import dev.openfeature.sdk.ImmutableStructure
import dev.openfeature.sdk.TrackingEventDetails
import dev.openfeature.sdk.Value
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