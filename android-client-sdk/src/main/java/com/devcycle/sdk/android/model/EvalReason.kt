package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(`as` = EvalReason::class)
data class EvalReason(
    /**
     * String that defines the high level reason for the evaluation
     */
    @get:Schema(required = true, description = "String that defines the high level reason for the evaluation")
    val reason: String = "",

    /**
     * String that defines the detailed reason for the evaluation
     */
    @get:Schema(required = false, description = "String that defines the detailed reason for the evaluation")
    val details: String? = null,

    /**
     * String that defines the target id for the evaluation
     */
    @get:Schema(required = false, description = "String that defines the target id for the evaluation")
    @JsonProperty("target_id")
    val targetId: String? = null,
) {
    companion object {
        fun defaultReason(details: String) = EvalReason("DEFAULT", details)
    }
}