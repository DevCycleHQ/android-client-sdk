package com.devcycle.sdk.android.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

internal class DVCApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(JacksonConverterFactory.create())

    fun initialize(): DVCApi {
        return adapterBuilder
            .client(okBuilder.build())
            .build()
            .create(DVCApi::class.java)
    }

    companion object {
        private const val BASE_URL = "https://sdk-api.devcycle.com/"
    }

}