package model

import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data

@Data
class ErrorResponse internal constructor(
    @field:Schema(
        required = true,
        description = "Error message"
    ) private val message: String?,
    @field:Schema(description = "Additional error information detailing the error reasoning") private val data: Any?
) {
    class ErrorResponseBuilder internal constructor() {
        private var message: String? = null
        private var data: Any? = null
        fun message(message: String?): ErrorResponseBuilder {
            this.message = message
            return this
        }

        fun data(data: Any?): ErrorResponseBuilder {
            this.data = data
            return this
        }

        fun build(): ErrorResponse {
            return ErrorResponse(message, data)
        }

        override fun toString(): String {
            return "ErrorResponse.ErrorResponseBuilder(message=$message, data=$data)"
        }
    }

    companion object {
        fun builder(): ErrorResponseBuilder {
            return ErrorResponseBuilder()
        }
    }
}