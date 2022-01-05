package com.devcycle.sdk.android.api

import android.util.Log
import com.devcycle.sdk.android.exception.DVCRequestException

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.io.IOException

internal class Request constructor(envKey: String) {
    private val TAG = "Request"
    private val api: DVCApi = DVCApiClient().initialize()
    private val eventApi: DVCEventsApi = DVCEventsApiClient().initialize(envKey)
    private val objectMapper = jacksonObjectMapper()
    private val configMutex = Mutex()

    suspend fun <T> retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 1000, // 1 second
        maxDelay: Long = 10000,    // 10 seconds
        factor: Double = 2.0,
        label: String? = null,
        block: suspend () -> T): T
    {
        var currentDelay = initialDelay
        while(times - 1 > 0 && currentDelay < maxDelay) {
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            val error: Throwable
            try {
                return block()
            } catch (e: DVCRequestException) {
                if (!e.isRetryable) {
                    throw e
                }
                error = e
            } catch (t: IOException) {
                error = t
            }

            Log.w(TAG, "Request " +
                    (label ?: "") + " Failed. Retrying in " + currentDelay / 1000 +  " seconds.", error)
            delay(currentDelay)
        }

        return block() // last attempt
    }

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

        configMutex.withLock {
            return retryIO(label="Config") {
                val response = api.getConfigJson(environmentKey, map)
                getResponseHandler(response)
            }
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