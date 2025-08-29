package com.devcycle.sdk.android.openfeature

import dev.openfeature.kotlin.sdk.Value
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class JsonValueConverterTest {

    @Test
    fun `convertToValue handles all basic types correctly`() {
        // Test basic types
        assertEquals(Value.String("test"), JsonValueConverter.convertToValue("test"))
        assertEquals(Value.Integer(42), JsonValueConverter.convertToValue(42))
        assertEquals(Value.Integer(42), JsonValueConverter.convertToValue(42L))
        assertEquals(Value.Double(3.14), JsonValueConverter.convertToValue(3.14))
        assertEquals(Value.Double(2.5), JsonValueConverter.convertToValue(2.5f))
        assertEquals(Value.Boolean(true), JsonValueConverter.convertToValue(true))
        assertEquals(Value.Boolean(false), JsonValueConverter.convertToValue(false))
        
        // Test null handling
        assertNull(JsonValueConverter.convertToValue(null))
        assertNull(JsonValueConverter.convertToValue(JSONObject.NULL))        
    }

    @Test
    fun `convertToValue handles JSONObject correctly`() {
        val jsonObject = JSONObject().apply {
            put("stringValue", "test string")
            put("intValue", 42)
            put("doubleValue", 3.14)
            put("booleanValue", true)
            put("nullValue", JSONObject.NULL)
        }

        val result = JsonValueConverter.convertToValue(jsonObject)
        assertTrue(result is Value.Structure)
        
        val structure = result as Value.Structure
        val map = structure.structure
        
        assertEquals(4, map?.size) // null values are filtered out
        assertEquals(Value.String("test string"), map?.get("stringValue"))
        assertEquals(Value.Integer(42), map?.get("intValue"))
        assertEquals(Value.Double(3.14), map?.get("doubleValue"))
        assertEquals(Value.Boolean(true), map?.get("booleanValue"))
        assertFalse(map?.containsKey("nullValue") ?: false) // null value should be filtered out
    }

    @Test
    fun `convertToValue handles JSONArray correctly`() {
        val jsonArray = JSONArray().apply {
            put("string item")
            put(42)
            put(true)
            put(3.14)
            put(JSONObject().apply { put("arrayObject", "value") })
        }

        val result = JsonValueConverter.convertToValue(jsonArray)
        assertTrue(result is Value.List)
        
        val list = result as Value.List
        val values = list.list ?: emptyList()
        
        assertEquals(5, values.size)
        assertEquals(Value.String("string item"), values[0])
        assertEquals(Value.Integer(42), values[1])
        assertEquals(Value.Boolean(true), values[2])
        assertEquals(Value.Double(3.14), values[3])
        assertTrue(values[4] is Value.Structure)
        
        val objectInArray = values[4] as Value.Structure
        val objectMap = objectInArray.structure
        assertEquals(Value.String("value"), objectMap?.get("arrayObject"))
    }

    @Test
    fun `convertJsonObjectToMap handles nested objects correctly`() {
        val nestedObject = JSONObject().apply {
            put("nestedString", "nested value")
            put("nestedNumber", 123)
        }
        
        val mainObject = JSONObject().apply {
            put("topLevel", "main value")
            put("nested", nestedObject)
        }

        val result = JsonValueConverter.convertJsonObjectToMap(mainObject)
        
        assertEquals(Value.String("main value"), result["topLevel"])
        assertTrue(result["nested"] is Value.Structure)
        
        val nestedStructure = result["nested"] as Value.Structure
        val nestedMap = nestedStructure.structure
        assertEquals(Value.String("nested value"), nestedMap?.get("nestedString"))
        assertEquals(Value.Integer(123), nestedMap?.get("nestedNumber"))
    }

    @Test
    fun `convertJsonArrayToList handles nested arrays correctly`() {
        val nestedArray = JSONArray().apply {
            put("nested item 1")
            put("nested item 2")
        }
        
        val mainArray = JSONArray().apply {
            put("main item")
            put(nestedArray)
            put(42)
        }

        val result = JsonValueConverter.convertJsonArrayToList(mainArray)
        
        assertEquals(3, result.size)
        assertEquals(Value.String("main item"), result[0])
        assertTrue(result[1] is Value.List)
        assertEquals(Value.Integer(42), result[2])
        
        val nestedList = result[1] as Value.List
        val nestedValues = nestedList.list ?: emptyList()
        assertEquals(2, nestedValues.size)
        assertEquals(Value.String("nested item 1"), nestedValues[0])
        assertEquals(Value.String("nested item 2"), nestedValues[1])
    }

    @Test
    fun `convertToValue handles complex nested structures correctly`() {
        val complexJson = JSONObject().apply {
            put("simpleString", "value")
            put("simpleNumber", 42)
            put("array", JSONArray().apply {
                put("item1")
                put(JSONObject().apply { put("nestedKey", "nestedValue") })
            })
            put("object", JSONObject().apply {
                put("level2", JSONObject().apply {
                    put("deepValue", "found")
                })
            })
        }

        val result = JsonValueConverter.convertToValue(complexJson)
        assertTrue(result is Value.Structure)
        
        val structure = result as Value.Structure
        val map = structure.structure
        
        assertEquals(Value.String("value"), map?.get("simpleString"))
        assertEquals(Value.Integer(42), map?.get("simpleNumber"))
        
        // Check array handling
        assertTrue(map?.get("array") is Value.List)
        val arrayValue = map?.get("array") as Value.List
        val arrayList = arrayValue.list ?: emptyList()
        assertEquals(2, arrayList.size)
        assertEquals(Value.String("item1"), arrayList[0])
        assertTrue(arrayList[1] is Value.Structure)
        
        // Check nested object handling
        assertTrue(map?.get("object") is Value.Structure)
        val objectValue = map?.get("object") as Value.Structure
        val objectMap = objectValue.structure
        assertTrue(objectMap?.get("level2") is Value.Structure)
        
        val level2Value = objectMap?.get("level2") as Value.Structure
        val level2Map = level2Value.structure
        assertEquals(Value.String("found"), level2Map?.get("deepValue"))
    }

    @Test
    fun `convertToValue handles empty structures correctly`() {
        // Test empty JSONObject
        val emptyObject = JSONObject()
        val objectResult = JsonValueConverter.convertToValue(emptyObject)
        
        assertTrue(objectResult is Value.Structure)
        val structure = objectResult as Value.Structure
        assertTrue(structure.structure?.isEmpty() ?: true)
        
        // Test empty JSONArray
        val emptyArray = JSONArray()
        val arrayResult = JsonValueConverter.convertToValue(emptyArray)
        
        assertTrue(arrayResult is Value.List)
        val list = arrayResult as Value.List
        assertTrue(list.list?.isEmpty() ?: true)
    }

    @Test
    fun `convertToValue handles unknown types by converting to string`() {
        class CustomObject {
            override fun toString(): String = "some complex value that doesn't match basic types"
        }
        
        val customObject = CustomObject()
        val result = JsonValueConverter.convertToValue(customObject)
        
        assertTrue(result is Value.String)
        assertEquals("some complex value that doesn't match basic types", (result as Value.String).asString())
    }

    @Test
    fun `convertJsonArrayToList filters out null values`() {
        val jsonArray = JSONArray().apply {
            put("valid string")
            put(42)
            put(JSONObject.NULL)
            put(true)
        }

        val result = JsonValueConverter.convertJsonArrayToList(jsonArray)
        
        assertEquals(3, result.size) // null values are filtered out
        assertEquals(Value.String("valid string"), result[0])
        assertEquals(Value.Integer(42), result[1])
        assertEquals(Value.Boolean(true), result[2])
    }
} 