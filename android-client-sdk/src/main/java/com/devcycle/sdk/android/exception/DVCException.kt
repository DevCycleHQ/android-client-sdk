package com.devcycle.sdk.android.exception

import com.devcycle.sdk.android.model.ErrorResponse
import com.devcycle.sdk.android.model.HttpResponseCode

class DVCException(httpResponseCode:HttpResponseCode, errorResponse: ErrorResponse): Exception(errorResponse.message) {
    private val httpResponseCode: HttpResponseCode? = null
    private val errorResponse: ErrorResponse? = null

    fun getHttpResponseCode(): HttpResponseCode? {
        return httpResponseCode
    }

    fun getErrorResponse(): ErrorResponse? {
        return errorResponse
    }
}