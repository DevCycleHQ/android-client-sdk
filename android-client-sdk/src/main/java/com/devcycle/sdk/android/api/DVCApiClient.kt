package com.devcycle.sdk.android.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class DVCApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()

    fun initialize(baseUrl: String, gsonMapper: Gson): DVCApi {
        return adapterBuilder
            .baseUrl(baseUrl)
            .client(okBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gsonMapper))
            .build()
            .create(DVCApi::class.java)
    }

    companion object {
        internal const val BASE_URL = "https://sdk-api.devcycle.com/"
    }
}