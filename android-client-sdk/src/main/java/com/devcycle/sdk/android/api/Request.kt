package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCConfigRequestException

import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.model.Event
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

internal class Request constructor(envKey: String) {
    private var api: DVCApi = DVCApiClient().initialize()
    private var eventApi: DVCEventsApi = DVCEventsApiClient().initialize(envKey)
    private val objectMapper = ObjectMapper()

    private fun <T>getResponseHandler(callback: DVCCallback<T?>?) = object : Callback<T?> {
        override fun onResponse(
            call: Call<T?>,
            response: Response<T?>
        ) {
            if (response.isSuccessful) {
                val config = response.body()
                callback?.onSuccess(config)
            } else {
                val httpResponseCode = HttpResponseCode.byCode(response.code())
                var errorResponse = ErrorResponse("Unknown Error", null)
                var dvcException = DVCConfigRequestException(httpResponseCode, errorResponse)
                if (response.errorBody() != null) {
                    try {
                        errorResponse = objectMapper.readValue(
                            response.errorBody()!!.string(),
                            ErrorResponse::class.java
                        )
                        dvcException = DVCConfigRequestException(httpResponseCode, errorResponse)
                    } catch (e: IOException) {
                        errorResponse.message = e.message
                        dvcException = DVCConfigRequestException(httpResponseCode, errorResponse)
                    }
                }
                callback?.onError(dvcException)
            }
        }

        override fun onFailure(call: Call<T?>, t: Throwable) {
            callback?.onError(t)
        }
    }

    @Synchronized
    fun getConfigJson(
        environmentKey: String,
        user: User,
        callback: DVCCallback<BucketedUserConfig?>
    ) {
        val map =
            objectMapper.convertValue(user, object : TypeReference<Map<String, String>>() {})
        val call = api.getConfigJson(environmentKey, map)
        call.enqueue(this.getResponseHandler(callback))
    }

    fun trackEvent(
        user: User,
        event: Event
    ) {
        val userAndEvents = UserAndEvents(user, mutableListOf(event))
        val call = eventApi.trackEvents(userAndEvents)
        call.enqueue(this.getResponseHandler(null))
    }

    init {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}