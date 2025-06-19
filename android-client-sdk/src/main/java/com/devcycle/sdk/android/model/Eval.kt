package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(`as` = Eval::class)
class Eval {
    /**
     * String that defines the high level reason for the evaluation
     */
    @get:Schema(required = true, description = "String that defines the high level reason for the evaluation")
    var reason: String = ""

    /**
     * String that defines the detailed reason for the evaluation
     */
    @get:Schema(required = false, description = "String that defines the detailed reason for the evaluation")
    var details: String? = null

    /**
     * String that defines the target id for the evaluation
     */
    @get:Schema(required = false, description = "String that defines the target id for the evaluation")
    @JsonProperty("target_id")
    var targetId: String? = null
}