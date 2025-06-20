package com.devcycle.sdk.android.model

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFormat

data class ErrorResponse (
    @Schema(required = true, description = "Error message")
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val message: List<String>? = null,
    @Schema(description = "Additional error information detailing the error reasoning")
    val error: String? = null,
    @Schema(description = "Status code of the response, also present in the response body")
    val statusCode: Number? = null
)