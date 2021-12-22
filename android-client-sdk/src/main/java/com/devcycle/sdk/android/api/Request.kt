package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCRequestException

import com.devcycle.sdk.android.model.*
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
                    throw DVCRequestException(httpResponseCode, errorResponse)
                } catch (e: IOException) {
                    errorResponse = ErrorResponse(e.message, null)
                    throw DVCRequestException(httpResponseCode, errorResponse)
                }
            }
            throw DVCRequestException(httpResponseCode, errorResponse)
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

    suspend fun publishEvents(payload: UserAndEvents): DVCResponse {
        val response = eventApi.trackEvents(payload)

        return getResponseHandler(response)
    }

    init {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}