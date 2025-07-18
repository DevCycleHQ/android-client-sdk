package com.devcycle.sdk.android.openfeature

import dev.openfeature.sdk.Value
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility class for converting JSON objects and values to OpenFeature Value types.
 */
object JsonValueConverter {
    
    /**
     * Converts a JSONObject to a Map of String to Value.
     */
    fun convertJsonObjectToMap(jsonObject: JSONObject): Map<String, Value> {
        val map = mutableMapOf<String, Value>()
        jsonObject.keys().forEach { key ->
            val value = jsonObject.get(key)
            convertToValue(value)?.let { convertedValue ->
                map[key] = convertedValue
            }
        }
        return map
    }

    /**
     * Converts a JSONArray to a List of Values.
     */
    fun convertJsonArrayToList(jsonArray: JSONArray): List<Value> {
        val list = mutableListOf<Value>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.get(i)
            convertToValue(value)?.let { convertedValue ->
                list.add(convertedValue)
            }
        }
        return list
    }

    /**
     * Converts any value to an OpenFeature Value, returning null for null values.
     */
    fun convertToValue(value: Any?): Value? = when (value) {
        is Boolean -> Value.Boolean(value)
        is Int -> Value.Integer(value)
        is Long -> Value.Integer(value.toInt())
        is Double -> Value.Double(value)
        is Float -> Value.Double(value.toDouble())
        is String -> Value.String(value)
        is JSONObject -> Value.Structure(convertJsonObjectToMap(value))
        is JSONArray -> Value.List(convertJsonArrayToList(value))
        null -> null
        JSONObject.NULL -> null
        else -> Value.String(value.toString())
    }
} 