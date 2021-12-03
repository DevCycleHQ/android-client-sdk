/*
 * DevCycle Client SDK API
 * Documents the DevCycle Client SDK API which powers bucketing and descisions for DevCycle's client SDKs.
 *
 * OpenAPI spec version: 1-oas3
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package com.devcycle.android.client.sdk.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Variable
 */
class Variable {
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
     * Variable type
     */
    enum class TypeEnum(@get:JsonValue val value: String) {
        STRING("String"), BOOLEAN("Boolean"), NUMBER("Number"), JSON("JSON");

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
     * Variable type
     * @return type
     */
    @get:Schema(required = true, description = "Variable type")
    var type: TypeEnum? = null

    /**
     * Variable value can be a string, number, boolean, or JSON
     * @return value
     */
    @get:Schema(
        required = true,
        description = "Variable value can be a string, number, boolean, or JSON"
    )
    var value: Any? = null
    fun id(id: String?): Variable {
        this.id = id
        return this
    }

    fun key(key: String?): Variable {
        this.key = key
        return this
    }

    fun type(type: TypeEnum?): Variable {
        this.type = type
        return this
    }

    fun value(value: Any?): Variable {
        this.value = value
        return this
    }
}