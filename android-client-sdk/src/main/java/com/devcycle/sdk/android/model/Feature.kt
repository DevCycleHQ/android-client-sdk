package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Feature
 */
data class Feature(
    /**
     * unique database id
     */
    @get:Schema(required = true, description = "unique database id")
    @JsonProperty("_id")
    val id: String? = null,

    /**
     * Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id.
     */
    @get:Schema(
        required = true,
        description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id."
    )
    val key: String? = null,

    /**
     * Feature type
     */
    @get:Schema(required = true, description = "Feature type")
    val type: TypeEnum? = null,

    /**
     * Bucketed feature variation ID
     */
    @get:Schema(required = true, description = "Bucketed feature variation")
    @JsonProperty("_variation")
    val variation: String? = null,

    /**
     * Evaluation reasoning
     */
    @get:Schema(description = "Evaluation reasoning")
    val evalReason: String? = null,

    /**
     * Variation name
     */
    @get:Schema(description = "Variation name")
    val variationName: String? = null,

    /**
     * Variation key
     */
    @get:Schema(description = "Variation key")
    val variationKey: String? = null
) {
    /**
     * Feature type
     */
    enum class TypeEnum(@get:JsonValue val value: String) {
        @JsonProperty("release")
        RELEASE("release"),
        @JsonProperty("experiment")
        EXPERIMENT("experiment"),
        @JsonProperty("permission")
        PERMISSION("permission"),
        @JsonProperty("ops")
        OPS("ops");

        override fun toString(): String {
            return value
        }

        companion object {
            fun fromValue(text: String): TypeEnum? {
                for (b in values()) {
                    if (b.value == text) {
                        return b
                    }
                }
                return null
            }
        }
    }
}