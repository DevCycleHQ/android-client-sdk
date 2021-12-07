package com.devcycle.android.client.sdk.api

import retrofit2.http.GET
import com.devcycle.android.client.sdk.model.BucketedUserConfig
import retrofit2.Call
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface DVCApi {
    @GET("/v1/sdkConfig")
    fun getConfigJson(
        @Query("envKey") envKey: String?,
        @QueryMap params: Map<String?, String?>?
    ): Call<BucketedUserConfig?>?
}