package com.devcycle.android.client.sdk.api

import com.devcycle.android.client.sdk.model.BucketedUserConfig
import com.devcycle.android.client.sdk.model.User
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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