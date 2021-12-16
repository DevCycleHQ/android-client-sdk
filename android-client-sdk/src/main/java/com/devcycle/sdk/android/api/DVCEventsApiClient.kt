package com.devcycle.sdk.android.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

internal class DVCEventsApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(JacksonConverterFactory.create())

    fun initialize(): DVCEventsApi {
        return adapterBuilder
            .client(okBuilder.build())
            .build()
            .create(DVCEventsApi::class.java)
    }

    companion object {
        private const val BASE_URL = "https://events.devcycle.com/"
    }
}