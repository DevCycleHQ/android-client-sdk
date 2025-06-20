package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * ProjectSettings
 */
data class ProjectSettings(
    /**
     * edgeDB Project Settings
     */
    @get:Schema(required = false, description = "edgeDB Project Settings")
    @JsonProperty("edgeDB")
    val edgeDB: EdgeDB? = null
)