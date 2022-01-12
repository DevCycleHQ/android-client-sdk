package com.devcycle.sdk.android.api

import android.util.Log
import com.devcycle.sdk.android.exception.DVCRequestException

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.IOException

internal class Request constructor(envKey: String, apiBaseUrl: String) {
    private val TAG = "Request"
    private val api: DVCApi = DVCApiClient().initialize(apiBaseUrl)
    private val eventApi: DVCEventsApi = DVCEventsApiClient().initialize(envKey)
    private val objectMapper = jacksonObjectMapper()
    private val configMutex = Mutex()

    private fun <T> getResponseHandler(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw Throwable("Unexpected result from API")
        } else {
            val httpResponseCode = HttpResponseCode.byCode(response.code())
            var errorResponse = ErrorResponse(listOf("Unknown Error"), null)

            if (response.errorBody() != null) {
                try {
                    errorResponse = objectMapper.readValue(
                        response.errorBody()!!.string(),
                        ErrorResponse::class.java
                    )
                    throw DVCRequestException(httpResponseCode, errorResponse)
                } catch (e: IOException) {
                    errorResponse = ErrorResponse(listOf(e.message ?: ""), null)
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

        var config: BucketedUserConfig? = null

        configMutex.withLock {
            var currentDelay = 1000L
            val delayFactor = 2
            val maxDelay = 10000L

            flow {
                val response = api.getConfigJson(environmentKey, map)
                emit(getResponseHandler(response))
            }
                .flowOn(Dispatchers.Default)
                .retryWhen { cause, attempt ->
                    if ((cause is DVCRequestException && !cause.isRetryable) || attempt > 4) {
                        return@retryWhen false
                    } else {
                        delay(currentDelay)
                        currentDelay = (currentDelay * delayFactor).coerceAtMost(maxDelay)
                        Log.w(TAG, "Request Config Failed. Retrying in ${currentDelay / 1000} seconds.", cause)
                        return@retryWhen true
                    }
                }
                .collect {
                    config = it
                }

            return config!!
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