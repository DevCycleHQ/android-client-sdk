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
package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Project
 */
class Project {
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
    fun id(id: String?): Project {
        this.id = id
        return this
    }

    fun key(key: String?): Project {
        this.key = key
        return this
    }
}