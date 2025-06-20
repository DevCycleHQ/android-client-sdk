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
) {

    class ErrorResponseBuilder internal constructor() {
        private var message: List<String>? = null
        private var error: String? = null
        private var statusCode: Number? = null

        fun message(message: List<String>): ErrorResponseBuilder {
            this.message = message
            return this
        }

        fun error(error: String): ErrorResponseBuilder {
            this.error = error
            return this
        }

        fun statusCode(statusCode: Number): ErrorResponseBuilder {
            this.statusCode = statusCode
            return this
        }


        fun build(): ErrorResponse {
            return ErrorResponse(message, error, statusCode)
        }
    }

    companion object {
        fun builder(): ErrorResponseBuilder {
            return ErrorResponseBuilder()
        }
    }
}