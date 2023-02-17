package com.devcycle.sdk.android.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthorizationHeaderInterceptor(private val sdkKey: String) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        request = request.newBuilder()
            .addHeader(AUTHORIZATION_HEADER, sdkKey)
            .build()
        return chain.proceed(request)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }
}