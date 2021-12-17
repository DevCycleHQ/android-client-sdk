package com.devcycle.sdk.android.api

import retrofit2.http.GET
import com.devcycle.sdk.android.model.BucketedUserConfig
import retrofit2.Call
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface DVCApi {
    @GET("/v1/mobileSDKConfig")
    fun getConfigJson(
        @Query("envKey") envKey: String,
        @QueryMap params: Map<String, String>
    ): Call<BucketedUserConfig?>
}