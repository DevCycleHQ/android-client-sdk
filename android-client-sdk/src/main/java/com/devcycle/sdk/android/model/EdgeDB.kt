package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * EdgeDB
 */
data class EdgeDB(
    /**
     * Enabled flag, that is set by the Bucketed User Config -> Project Settings -> EdgeDB
     */
    @get:Schema(required = false, description = "flag that determines whether or not EdgeDB is enabled")
    @JsonProperty("enabled")
    val enabled: Boolean? = false
)