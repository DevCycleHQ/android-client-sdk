package com.devcycle.sdk.android.exception

import com.devcycle.sdk.android.model.ErrorResponse
import com.devcycle.sdk.android.model.HttpResponseCode

class DVCRequestException(
    private val httpResponseCode:HttpResponseCode,
    private val errorResponse: ErrorResponse): Exception(errorResponse.message) {

    fun getHttpResponseCode(): HttpResponseCode {
        return httpResponseCode
    }

    fun getErrorResponse(): ErrorResponse {
        return errorResponse
    }

    val isRetryable get() = httpResponseCode.code() >= 500
}