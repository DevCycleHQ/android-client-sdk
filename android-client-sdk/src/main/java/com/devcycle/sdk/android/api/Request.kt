package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.exception.DVCException
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.ErrorResponse
import com.devcycle.sdk.android.model.HttpResponseCode
import com.devcycle.sdk.android.model.User
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

internal class Request {
    private var api: DVCApi? = null
    private val objectMapper = ObjectMapper()
    @Synchronized
    fun getConfigJson(
        environmentKey: String?,
        user: User?,
        callback: DVCCallback<BucketedUserConfig?>
    ) {
        val map =
            objectMapper.convertValue(user, object : TypeReference<Map<String?, String?>?>() {})
        val call = api!!.getConfigJson(environmentKey, map)
        call!!.enqueue(object : Callback<BucketedUserConfig?> {
            override fun onResponse(
                call: Call<BucketedUserConfig?>,
                response: Response<BucketedUserConfig?>
            ) {
                if (response.isSuccessful) {
                    val config = response.body()
                    callback.onSuccess(config)
                } else {
                    val httpResponseCode = HttpResponseCode.byCode(response.code())
                    var errorResponse = ErrorResponse("Unknown Error", null)
                    var dvcException = DVCException(httpResponseCode, errorResponse)
                    if (response.errorBody() != null) {
                        try {
                            errorResponse = objectMapper.readValue(
                                response.errorBody()!!.string(),
                                ErrorResponse::class.java
                            )
                            dvcException = DVCException(httpResponseCode, errorResponse)
                        } catch (e: IOException) {
                            errorResponse.message = e.message
                            dvcException = DVCException(httpResponseCode, errorResponse)
                        }
                    }
                    callback.onError(dvcException)
                }
            }

            override fun onFailure(call: Call<BucketedUserConfig?>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    init {
        api = DVCApiClient().initialize()
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}