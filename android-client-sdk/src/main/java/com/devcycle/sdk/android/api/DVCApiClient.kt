package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.util.JSONMapper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

internal class DVCApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(JSONMapper.mapper))

    fun initialize(baseUrl: String): DVCApi {
        return adapterBuilder
            .baseUrl(baseUrl)
            .client(okBuilder.build())
            .build()
            .create(DVCApi::class.java)
    }

    companion object {
        internal const val BASE_URL = "https://sdk-api.devcycle.com/"
    }
}