package com.devcycle.sdk.android.exception

import com.devcycle.sdk.android.model.ErrorResponse
import com.devcycle.sdk.android.model.HttpResponseCode

class DVCRequestException(
    val statusCode: Int,
    private val errorResponse: ErrorResponse
): Exception(errorResponse.message?.getOrNull(0)) {

    private val httpResponseCode = HttpResponseCode.byCode(statusCode)

    fun getHttpResponseCode(): HttpResponseCode = httpResponseCode

    fun getErrorResponse(): ErrorResponse = errorResponse

    val isRetryable get() = statusCode == 429 || statusCode >= 500

    val isAuthError get() = statusCode == 401 || statusCode == 403
}