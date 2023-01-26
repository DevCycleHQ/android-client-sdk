package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.interceptor.AuthorizationHeaderInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class DVCEventsApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())

    fun initialize(envKey: String, baseUrl: String): DVCEventsApi {
        okBuilder.addInterceptor(AuthorizationHeaderInterceptor(envKey))
        return adapterBuilder
            .baseUrl(baseUrl)
            .client(okBuilder.build())
            .build()
            .create(DVCEventsApi::class.java)
    }

    companion object {
        internal const val BASE_URL = "https://events.devcycle.com/"
    }
}