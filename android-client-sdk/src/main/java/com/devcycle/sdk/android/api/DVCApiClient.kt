package com.devcycle.sdk.android.api

import android.content.Context
import com.devcycle.sdk.android.interceptor.NetworkConnectionInterceptor
import com.devcycle.sdk.android.util.JSONMapper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal class DVCApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(JSONMapper.mapper))

    fun initialize(baseUrl: String, context: Context): DVCApi {
        okBuilder.addInterceptor(NetworkConnectionInterceptor(context))
        okBuilder.connectTimeout(30, TimeUnit.SECONDS)
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