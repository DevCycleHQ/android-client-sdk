/*
 * DevCycle Client SDK API
 * Documents the DevCycle Client SDK API which powers bucketing and decisions for DevCycle's client SDKs.
 *
 * OpenAPI spec version: 1-oas3
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package com.devcycle.sdk.android.model

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
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