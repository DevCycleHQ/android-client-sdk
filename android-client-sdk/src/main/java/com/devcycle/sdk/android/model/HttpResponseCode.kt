package com.devcycle.sdk.android.model

enum class HttpResponseCode(val code: Int) {
    OK(200),
    ACCEPTED(201),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    TOO_MANY_REQUESTS(429),
    SERVER_ERROR(500);

    companion object {
        fun byCode(code: Int): HttpResponseCode {
            for (httpResponseCode in values()) {
                if (httpResponseCode.code == code) {
                    return httpResponseCode
                }
            }
            return SERVER_ERROR
        }
    }
}