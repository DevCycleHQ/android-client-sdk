package com.devcycle.android.client.sdk.exception

import com.devcycle.android.client.sdk.model.ErrorResponse
import com.devcycle.android.client.sdk.model.HttpResponseCode
import lombok.Getter

@Getter
class DVCException {
    private val httpResponseCode: HttpResponseCode? = null
    private val errorResponse: ErrorResponse? = null
}