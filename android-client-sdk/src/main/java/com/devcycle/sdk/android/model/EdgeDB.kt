package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)

class EdgeDB {
    /**
     * Enabled flag, that is set by the Bucketed User Config -> Project Settings -> EdgeDB
     * @return _id
     */
    @get:Schema(required = false, description = "flag that determines whether or not EdgeDB is enabled")
    @JsonProperty("enabled")
    var enabled: Boolean? = false

    fun enabled(enabled: Boolean): EdgeDB {
        this.enabled = enabled
        return this
    }
}