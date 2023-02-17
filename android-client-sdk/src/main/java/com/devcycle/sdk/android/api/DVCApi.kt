package com.devcycle.sdk.android.api

import retrofit2.http.GET
import com.devcycle.sdk.android.model.BucketedUserConfig
import retrofit2.Response
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface DVCApi {
    @GET("/v1/mobileSDKConfig")
    suspend fun getConfigJson(
        @Query("sdkKey") sdkKey: String,
        @QueryMap params: Map<String, String>
    ): Response<BucketedUserConfig>
}