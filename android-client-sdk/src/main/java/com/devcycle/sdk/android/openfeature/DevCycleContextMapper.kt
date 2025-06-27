package com.devcycle.sdk.android.openfeature

import com.devcycle.sdk.android.model.DevCycleUser
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.Value

object DevCycleContextMapper {
    
    fun evaluationContextToDevCycleUser(context: EvaluationContext?): DevCycleUser? {
        if (context == null) return null
        
        val builder = DevCycleUser.builder()
        var hasTargetingKey = false
        var hasStandardAttributes = false
        
        // Map targeting key to user ID if available - using try/catch for API compatibility
        try {
            val targetingKey = context.javaClass.getMethod("getTargetingKey").invoke(context) as? String
            targetingKey?.let { 
                if (it.isNotBlank()) {
                    builder.withUserId(it)
                    hasTargetingKey = true
                }
            }
        } catch (e: Exception) {
            // Handle API differences gracefully
        }
        
        // Map standard attributes
        context.getValue("email")?.let { email ->
            if (email is Value.String) {
                email.asString()?.let { emailStr ->
                    builder.withEmail(emailStr)
                    hasStandardAttributes = true
                }
            }
        }
        
        context.getValue("name")?.let { name ->
            if (name is Value.String) {
                name.asString()?.let { nameStr ->
                    builder.withName(nameStr)
                    hasStandardAttributes = true
                }
            }
        }
        
        context.getValue("country")?.let { country ->
            if (country is Value.String) {
                country.asString()?.let { countryStr ->
                    builder.withCountry(countryStr)
                    hasStandardAttributes = true
                }
            }
        }
        
        context.getValue("isAnonymous")?.let { isAnonymous ->
            if (isAnonymous is Value.Boolean) {
                isAnonymous.asBoolean()?.let { isAnonBool ->
                    builder.withIsAnonymous(isAnonBool)
                    hasStandardAttributes = true
                }
            }
        }
        
        // Map custom data
        val customData = mutableMapOf<String, Any>()
        val privateCustomData = mutableMapOf<String, Any>()
        
        try {
            // Use reflection to safely access asMap method
            val asMapMethod = context.javaClass.getMethod("asMap")
            val contextMap = asMapMethod.invoke(context) as? Map<String, Value>
            contextMap?.forEach { (key, value) ->
                when (key) {
                    "email" -> {
                        // Only skip if it was successfully processed as a string above
                        if (value !is Value.String) {
                            val convertedValue = convertValueToAny(value)
                            if (convertedValue != null) {
                                customData[key] = convertedValue
                            }
                        }
                    }
                    "name" -> {
                        // Only skip if it was successfully processed as a string above
                        if (value !is Value.String) {
                            val convertedValue = convertValueToAny(value)
                            if (convertedValue != null) {
                                customData[key] = convertedValue
                            }
                        }
                    }
                    "country" -> {
                        // Only skip if it was successfully processed as a string above
                        if (value !is Value.String) {
                            val convertedValue = convertValueToAny(value)
                            if (convertedValue != null) {
                                customData[key] = convertedValue
                            }
                        }
                    }
                    "isAnonymous" -> {
                        // Only skip if it was successfully processed as a boolean above
                        if (value !is Value.Boolean) {
                            val convertedValue = convertValueToAny(value)
                            if (convertedValue != null) {
                                customData[key] = convertedValue
                            }
                        }
                    }
                    else -> {
                        val convertedValue = convertValueToAny(value)
                        if (convertedValue != null) {
                            if (key.startsWith("private_")) {
                                privateCustomData[key.removePrefix("private_")] = convertedValue
                            } else {
                                customData[key] = convertedValue
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle any API issues gracefully
        }
        
        if (customData.isNotEmpty()) {
            builder.withCustomData(customData)
        }
        
        if (privateCustomData.isNotEmpty()) {
            builder.withPrivateCustomData(privateCustomData)
        }
        
        // Only return a user if we have meaningful data
        return if (hasTargetingKey || hasStandardAttributes || customData.isNotEmpty() || privateCustomData.isNotEmpty()) {
            builder.build()
        } else {
            null
        }
    }
    
    private fun convertValueToAny(value: Value): Any? {
        return when (value) {
            is Value.Boolean -> value.asBoolean()
            is Value.Integer -> value.asInteger()
            is Value.Double -> value.asDouble()
            is Value.String -> value.asString()
            is Value.Structure -> {
                try {
                    // Access structure directly
                    val structureMap = value.structure
                    structureMap?.mapValues { (_, v) -> convertValueToAny(v) }?.filterValues { it != null }
                } catch (e: Exception) {
                    null
                }
            }
            is Value.List -> {
                try {
                    val list = value.list
                    list?.mapNotNull { convertValueToAny(it) }
                } catch (e: Exception) {
                    null
                }
            }
            else -> value.toString()
        }
    }
} 