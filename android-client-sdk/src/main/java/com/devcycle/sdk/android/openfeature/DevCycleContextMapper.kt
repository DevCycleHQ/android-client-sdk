package com.devcycle.sdk.android.openfeature

import com.devcycle.sdk.android.model.DevCycleUser
import com.devcycle.sdk.android.util.DevCycleLogger
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.Value

object DevCycleContextMapper {
    
    fun evaluationContextToDevCycleUser(context: EvaluationContext?): DevCycleUser? {
        if (context == null) return null
        
        val builder = DevCycleUser.builder()
        var hasTargetingKey = false
        var hasStandardAttributes = false
        var isAnonymousExplicitlySet = false
        
        // Map targeting key to user ID if available (highest priority)
        context.getTargetingKey()?.let { targetingKey ->
            if (targetingKey.isNotBlank()) {
                builder.withUserId(targetingKey)
                hasTargetingKey = true
            }
        }
        
        // If no targeting key, try to get user ID from context values
        if (!hasTargetingKey) {
            // Check for "userId" first
            context.getValue("userId")?.let { userId ->
                if (userId is Value.String) {
                    userId.asString()?.let { userIdStr ->
                        if (userIdStr.isNotBlank()) {
                            builder.withUserId(userIdStr)
                            hasTargetingKey = true
                        }
                    }
                }
            }
            
            // If still no user ID, check for "user_id"
            if (!hasTargetingKey) {
                context.getValue("user_id")?.let { userId ->
                    if (userId is Value.String) {
                        userId.asString()?.let { userIdStr ->
                            if (userIdStr.isNotBlank()) {
                                builder.withUserId(userIdStr)
                                hasTargetingKey = true
                            }
                        }
                    }
                }
            }
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
                    isAnonymousExplicitlySet = true
                }
            }
        }
        
        // Map custom data
        val customData = mutableMapOf<String, Any>()
        val privateCustomData = mutableMapOf<String, Any>()
        
        // Use direct asMap method call instead of reflection
        context.asMap().forEach { (key, value) ->
            when (key) {
                "userId", "user_id" -> {
                    // Skip these if they were already processed for user ID
                    if (!hasTargetingKey || value !is Value.String) {
                        // Only add to custom data if not used as user ID or if wrong type
                        val convertedValue = convertValueToAny(value)
                        if (convertedValue != null) {
                            customData[key] = convertedValue
                        }
                    }
                }
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
                "customData" -> {
                    // Handle nested customData structure by flattening it
                    if (value is Value.Structure) {
                        val structureMap = convertValueToAny(value) as? Map<*, *>
                        structureMap?.forEach { (nestedKey, nestedValue) ->
                            if (nestedKey is String && nestedValue != null) {
                                customData[nestedKey] = nestedValue
                            }
                        }
                    } else {
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
        
        if (customData.isNotEmpty()) {
            builder.withCustomData(customData)
        }
        
        if (privateCustomData.isNotEmpty()) {
            builder.withPrivateCustomData(privateCustomData)
        }
        
        // Only return a user if we have meaningful data
        return if (hasTargetingKey || hasStandardAttributes || customData.isNotEmpty() || privateCustomData.isNotEmpty()) {
            // If user has a targeting key, they should be considered identified (not anonymous)
            // unless explicitly set to anonymous via a boolean value
            if (hasTargetingKey && !isAnonymousExplicitlySet) {
                builder.withIsAnonymous(false)
            }            
        
            return builder.build()
        } else {
            return null
        }
    }
    
    private fun convertValueToAny(value: Value): Any? {
        return when (value) {
            is Value.Boolean -> value.asBoolean()
            is Value.Integer -> value.asInteger()
            is Value.Double -> value.asDouble()
            is Value.String -> value.asString()
            is Value.Structure -> {
                // Access structure directly
                val structureMap = value.structure
                structureMap?.mapValues { (_, v) -> 
                    convertValueToAny(v) 
                }?.filterValues { it != null }
            }
            is Value.List -> {
                // Access list directly
                val list = value.list
                list?.mapNotNull { convertValueToAny(it) } ?: emptyList<Any>()
            }
            else -> {
                // Ensure the string representation is safe
                val stringValue = value.toString()
                if (stringValue.isNotBlank()) stringValue else null
            }
        }
    }
} 