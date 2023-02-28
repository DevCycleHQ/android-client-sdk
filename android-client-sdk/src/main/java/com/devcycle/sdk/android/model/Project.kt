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

    /**
     * Project Settings.
     * @return settings
     */
    @get:Schema(
        required = false,
        description = "Settings by Project."
    )
    var settings: ProjectSettings? = null

    fun id(id: String?): Project {
        this.id = id
        return this
    }

    fun key(key: String?): Project {
        this.key = key
        return this
    }

    fun settings(settings: ProjectSettings?): Project {
        this.settings = settings
        return this
    }
}