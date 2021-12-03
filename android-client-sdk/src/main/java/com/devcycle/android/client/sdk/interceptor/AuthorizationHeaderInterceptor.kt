package com.devcycle.android.client.sdk.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthorizationHeaderInterceptor(private val apiKey: String) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        request = request.newBuilder()
            .addHeader(AUTHORIZATION_HEADER, apiKey)
            .build()
        return chain.proceed(request)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }
}