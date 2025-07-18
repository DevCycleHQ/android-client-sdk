package com.devcycle.sdk.android.openfeature

import com.devcycle.sdk.android.model.DevCycleEvent
import com.devcycle.sdk.android.util.DevCycleLogger
import dev.openfeature.sdk.TrackingEventDetails
import dev.openfeature.sdk.Value
import java.math.BigDecimal

object DevCycleEventMapper {
    
    /**
     * Converts OpenFeature tracking event details to a DevCycle event
     * 
     * @param trackingEventName The name/type of the event to track
     * @param details Optional tracking event details containing value and metadata
     * @return DevCycleEvent ready to be tracked
     */
    fun openFeatureEventToDevCycleEvent(
        trackingEventName: String,
        details: TrackingEventDetails?
    ): DevCycleEvent {
        val builder = DevCycleEvent.builder().withType(trackingEventName)
        
        // Process numeric value if provided
        details?.value?.let { value ->
            builder.withValue(BigDecimal.valueOf(value.toDouble()))
        }
        
        // Process metadata from structure if provided - unwrap Value objects to raw values
        details?.structure?.asMap()?.let { detailData ->
            val metadata = mutableMapOf<String, Any>()
            detailData.forEach { (key, value) ->
                val unwrappedValue = unwrapValue(value)
                if (unwrappedValue != null) {
                    metadata[key] = unwrappedValue
                }
            }
            if (metadata.isNotEmpty()) {
                builder.withMetaData(metadata)
            }
        }
        
        return builder.build()
    }
    
    /**
     * Unwraps OpenFeature Value objects to raw Java types recursively
     */
    private fun unwrapValue(value: Any): Any? {
        return when (value) {
            is Value.String -> value.asString()
            is Value.Boolean -> value.asBoolean()
            is Value.Integer -> value.asInteger()
            is Value.Double -> value.asDouble()
            is Value.Structure -> {
                // Recursively unwrap nested structure
                val structureMap = value.structure
                structureMap?.mapValues { (_, v) -> 
                    unwrapValue(v) 
                }?.filterValues { it != null }
            }
            is Value.List -> {
                // Recursively unwrap nested list
                val list = value.list
                list?.mapNotNull { unwrapValue(it) }
            }
            else -> null
        }
    }
} 