package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Feature
 */
class Feature {
    /**
     * unique database id
     * @return _id
     */
    @get:Schema(required = true, description = "unique database id")
    @JsonProperty("_id")
    var id: String? = null

    /**
     * Unique key by Project, can be used in the SDK / API to reference by &#x27;key&#x27; rather than _id.
     * @return key
     */
    @get:Schema(
        required = true,
        description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id."
    )
    var key: String? = null

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

    /**
     * Feature type
     * @return type
     */
    @get:Schema(required = true, description = "Feature type")
    var type: TypeEnum? = null

    /**
     * Bucketed feature variation ID
     * @return _variation
     */
    @get:Schema(required = true, description = "Bucketed feature variation")
    @JsonProperty("_variation")
    var variation: String? = null

    /**
     * Evaluation reasoning
     * @return evalReason
     */
    @get:Schema(description = "Evaluation reasoning")
    var evalReason: String? = null

    /**
     * Variation name
     * @return variationName
     */
    @get:Schema(description = "Variation name")
    var variationName: String? = null

    /**
     * Variation key
     * @return variationKey
     */
    @get:Schema(description = "Variation key")
    var variationKey: String? = null

    fun id(id: String?): Feature {
        this.id = id
        return this
    }

    fun key(key: String?): Feature {
        this.key = key
        return this
    }

    fun type(type: TypeEnum?): Feature {
        this.type = type
        return this
    }

    fun variation(variation: String?): Feature {
        this.variation = variation
        return this
    }

    fun evalReason(evalReason: String?): Feature {
        this.evalReason = evalReason
        return this
    }

    fun variationName(variationName: String?): Feature {
        this.variationName = variationName
        return this
    }

    fun variationKey(variationKey: String?): Feature {
        this.variationKey = variationKey
        return this
    }
}