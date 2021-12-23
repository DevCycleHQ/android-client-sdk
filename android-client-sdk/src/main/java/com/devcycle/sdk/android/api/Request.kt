package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCConfigRequestException

import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.model.Event
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.IOException

internal class Request constructor(envKey: String) {
    private val api: DVCApi = DVCApiClient().initialize()
    private val eventApi: DVCEventsApi = DVCEventsApiClient().initialize(envKey)
    private val objectMapper = jacksonObjectMapper()
    private val configMutex = Mutex()

    private fun <T> getResponseHandler(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw Throwable("Unexpected result from API")
        } else {
            val httpResponseCode = HttpResponseCode.byCode(response.code())
            var errorResponse = ErrorResponse("Unknown Error", null)
            if (response.errorBody() != null) {
                try {
                    errorResponse = objectMapper.readValue(
                        response.errorBody()!!.string(),
                        ErrorResponse::class.java
                    )
                    throw DVCConfigRequestException(httpResponseCode, errorResponse)
                } catch (e: IOException) {
                    errorResponse = ErrorResponse(e.message, null)
                    throw DVCConfigRequestException(httpResponseCode, errorResponse)
                }
            }
            throw DVCConfigRequestException(httpResponseCode, errorResponse)
        }
    }

    suspend fun getConfigJson(
        environmentKey: String,
        user: User
    ): BucketedUserConfig {
        val map =
            objectMapper.convertValue(user, object : TypeReference<Map<String, String>>() {})

        configMutex.withLock {
            val response = api.getConfigJson(environmentKey, map)
            return getResponseHandler(response)
        }
    }

    suspend fun publishEvents(user: User, events: MutableList<Event>): DVCResponse {
        val userAndEvents = UserAndEvents(user, events)
        val response = eventApi.trackEvents(userAndEvents)

        return getResponseHandler(response)
    }

    init {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}