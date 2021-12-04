package com.devcycle.android.client.sdk.exception

import com.devcycle.android.client.sdk.model.ErrorResponse
import com.devcycle.android.client.sdk.model.HttpResponseCode

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