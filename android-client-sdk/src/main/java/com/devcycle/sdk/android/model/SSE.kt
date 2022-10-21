package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)

class SSE {
    /**
     * SSE connection URL, that is set by the Bucketed User Config -> SSE -> URL
     */
    @get:Schema(required = false, description = "flag that determines whether or not EdgeDB is enabled")
    @JsonProperty("url")
    var url: String? = null
}