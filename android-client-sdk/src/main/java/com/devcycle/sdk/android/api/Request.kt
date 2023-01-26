package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCRequestException

import com.devcycle.sdk.android.model.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

internal class Request constructor(envKey: String, apiBaseUrl: String, eventsBaseUrl: String) {
    private val eventApi: DVCEventsApi = DVCEventsApiClient().initialize(envKey, eventsBaseUrl)
    private val edgeDBApi: DVCEdgeDBApi = DVCEdgeDBApiClient().initialize(envKey, apiBaseUrl)
    private val objectMapper: ObjectMapper = ObjectMapper()
    private val gsonMapper = getGsonMapper();
    private val api: DVCApi = DVCApiClient().initialize(apiBaseUrl, gsonMapper)
    private val configMutex = Mutex()

    private fun getGsonMapper(): Gson {
        return GsonBuilder().registerTypeAdapter(Variable.TypeEnum::class.java, object : TypeAdapter<Variable.TypeEnum>() {
            override fun read(jsonReader: JsonReader): Variable.TypeEnum? {
                val jsonValue = jsonReader.nextString()
                return Variable.TypeEnum.values().firstOrNull { it.value == jsonValue }
            }

            override fun write(jsonWriter: JsonWriter, typeEnum: Variable.TypeEnum) {
                jsonWriter.value(typeEnum.value)
            }
        }).registerTypeAdapter(Feature.TypeEnum::class.java, object : TypeAdapter<Feature.TypeEnum>() {
            override fun read(jsonReader: JsonReader): Feature.TypeEnum? {
                val jsonValue = jsonReader.nextString()
                return Feature.TypeEnum.values().firstOrNull { it.value == jsonValue }
            }

            override fun write(jsonWriter: JsonWriter, typeEnum: Feature.TypeEnum) {
                jsonWriter.value(typeEnum.value)
            }
        }).create();
    }


    private fun <T> getResponseHandler(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw Throwable("Unexpected result from API")
        } else {
            val httpResponseCode = HttpResponseCode.byCode(response.code())
            var errorResponse = ErrorResponse(listOf("Unknown Error"), null)

            if (response.errorBody() != null) {
                try {
                    errorResponse = gsonMapper.fromJson(
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
        user: PopulatedUser,
        enableEdgeDB: Boolean,
        sse: Boolean? = false,
        lastModified: Long? = null
    ): BucketedUserConfig {
        val map: MutableMap<String, String> =
            gsonMapper.fromJson(gsonMapper.toJson(user), MutableMap::class.java) as MutableMap<String, String>

        if(map.containsKey("customData")) {
            map["customData"] = objectMapper.writeValueAsString(map["customData"])
        }
        if(map.containsKey("privateCustomData")) {
            map["privateCustomData"] = objectMapper.writeValueAsString(map["privateCustomData"])
        }
        if (enableEdgeDB) {
            map["enableEdgeDB"] = "true"
        }
        if(sse == true) {
            map["sse"] = "true"
        }
        if(lastModified != null) {
            map["sseLastModified"] = "$lastModified"
        }

        lateinit var config: BucketedUserConfig

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
                        Timber.w(
                            cause,
                            "Request Config Failed. Retrying in %s seconds.", currentDelay / 1000
                        )
                        return@retryWhen true
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}