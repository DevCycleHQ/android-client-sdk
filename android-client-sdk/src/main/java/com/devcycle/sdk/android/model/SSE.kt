package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

class SSE {
    /**
     * SSE connection URL, that is set by the Bucketed User Config -> SSE -> URL
     */
    @get:Schema(required = false, description = "String that tells the SDK at which SSE url to connect")
    @JsonProperty("url")
    var url: String? = null
    /**
     * Delay before disconnecting sse connetion, that is set by the Bucketed User Config -> SSE -> inactivityDelay
     */
    @get:Schema(required = false, description = "Tells the sdk how long the app can be inactive for before closing the sse connection")
    @JsonProperty("inactivityDelay")
    var inactivityDelay: Int? = null
}