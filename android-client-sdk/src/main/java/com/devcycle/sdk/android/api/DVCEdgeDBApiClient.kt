package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.interceptor.AuthorizationHeaderInterceptor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

internal class DVCEdgeDBApiClient {
    private val okBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    private val objectMapper = jacksonObjectMapper()
    private val adapterBuilder: Retrofit.Builder = Retrofit.Builder()
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))

    fun initialize(envKey: String, baseUrl: String): DVCEdgeDBApi {
        okBuilder.addInterceptor(AuthorizationHeaderInterceptor(envKey))
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