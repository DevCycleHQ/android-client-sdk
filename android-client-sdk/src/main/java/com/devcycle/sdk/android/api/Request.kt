package com.devcycle.sdk.android.api

import android.content.Context
import com.devcycle.sdk.android.exception.DVCRequestException

import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.JSONMapper
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import com.devcycle.sdk.android.util.DVCLogger
import java.io.IOException

internal class Request constructor(sdkKey: String, apiBaseUrl: String, eventsBaseUrl: String, context: Context) {
    private val api: DVCApi = DVCApiClient().initialize(apiBaseUrl, context)
    private val eventApi: DVCEventsApi = DVCEventsApiClient().initialize(sdkKey, eventsBaseUrl)
    private val edgeDBApi: DVCEdgeDBApi = DVCEdgeDBApiClient().initialize(sdkKey, apiBaseUrl, context)
    private val configMutex = Mutex()

    private fun <T> getResponseHandler(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw Throwable("Unexpected result from API")
        } else {
            val httpResponseCode = HttpResponseCode.byCode(response.code())
            var errorResponse = ErrorResponse(listOf("Unknown Error"), null)

            if (response.errorBody() != null) {
                try {
                    errorResponse = JSONMapper.mapper.readValue(
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
        sdkKey: String,
        user: PopulatedUser,
        enableEdgeDB: Boolean,
        sse: Boolean? = false,
        lastModified: Long? = null,
        etag: String? = null
    ): BucketedUserConfig {
        val map = (
            JSONMapper.mapper.convertValue(user, object : TypeReference<Map<String, Any>>() {})
        ) as MutableMap<String, String>

        if (map.contains("customData")) {
            map["customData"] = JSONMapper.mapper.writeValueAsString(map["customData"])
        }
        if (map.contains("privateCustomData")) {
            map["privateCustomData"] = JSONMapper.mapper.writeValueAsString(map["privateCustomData"])
        }
        if (enableEdgeDB) {
            map["enableEdgeDB"] = "true"
        }
        if (sse == true) {
            map["sse"] = "true"
        }
        if (lastModified != null) {
            map["sseLastModified"] = "$lastModified"
        }
        if (etag != null) {
            map["sseEtag"] = etag
        }

        lateinit var config: BucketedUserConfig

        configMutex.withLock {
            var currentDelay = 1000L
            val delayFactor = 2
            val maxDelay = 10000L

            flow {
                val response = api.getConfigJson(sdkKey, map)
                emit(getResponseHandler(response))
            }
                .flowOn(Dispatchers.Default)
                .retryWhen { cause, attempt ->
                    if (cause is DVCRequestException) {
                        if (!cause.isRetryable || attempt > 4) {
                            return@retryWhen false
                        } else {
                            delay(currentDelay)
                            currentDelay = (currentDelay * delayFactor).coerceAtMost(maxDelay)
                            DVCLogger.w(
                                cause,
                                "Request Config Failed. Retrying in %s seconds.", currentDelay / 1000
                            )
                            return@retryWhen true
                        }
                    } else {
                        DVCLogger.e(cause, cause.message)
                        return@retryWhen false
                    }
                }
                .collect{
                    config = it
                }
        }
        return config
    }

    suspend fun publishEvents(payload: UserAndEvents): DVCResponse {
        val response = eventApi.trackEvents(payload)

        return getResponseHandler(response)
    }

    suspend fun saveEntity(user: PopulatedUser): DVCResponse {
        val response = edgeDBApi.saveEntity(user.userId, user)

        return getResponseHandler(response)
    }

    init {
        JSONMapper.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}