package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Project
 */
data class Project(
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
     * Project Settings.
     */
    @get:Schema(
        required = false,
        description = "Settings by Project."
    )
    val settings: ProjectSettings? = null
)