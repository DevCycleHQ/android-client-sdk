package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.interceptor.AuthorizationHeaderInterceptor
import com.devcycle.sdk.android.util.JSONMapper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

internal class DVCEdgeDBApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(JSONMapper.mapper))

    fun initialize(sdkKey: String, baseUrl: String): DVCEdgeDBApi {
        okBuilder.addInterceptor(AuthorizationHeaderInterceptor(sdkKey))
        return adapterBuilder
            .baseUrl(baseUrl)
            .client(okBuilder.build())
            .build()
            .create(DVCEdgeDBApi::class.java)
    }

    companion object {
        internal const val BASE_URL = "https://sdk-api.devcycle.com/"
    }
}