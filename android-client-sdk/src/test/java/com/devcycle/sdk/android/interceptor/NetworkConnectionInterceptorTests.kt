package com.devcycle.sdk.android.interceptor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.devcycle.sdk.android.util.JSONMapper
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetworkConnectionInterceptorTests {
    private var mockWebServer: MockWebServer = MockWebServer()

    private val mockContext: Context = Mockito.mock(Context::class.java)
    private val mockConnectivityManager: ConnectivityManager = Mockito.mock(ConnectivityManager::class.java)
    private val mockNetworkCapabilities: NetworkCapabilities = Mockito.mock(NetworkCapabilities::class.java)

    private lateinit var client: OkHttpClient
    private lateinit var retrofit: Retrofit

    @BeforeAll
    fun setup() {
        client = OkHttpClient.Builder()
            .addInterceptor(NetworkConnectionInterceptor(mockContext))
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(JacksonConverterFactory.create(JSONMapper.mapper))
            .client(client)
            .build()
        Mockito.`when`(mockNetworkCapabilities.hasTransport(anyInt())).thenReturn(true)
        Mockito.`when`(mockConnectivityManager.getNetworkCapabilities(mockConnectivityManager.activeNetwork)).thenReturn(mockNetworkCapabilities)
        Mockito.`when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mockConnectivityManager)
    }

    @AfterAll
    fun close() {
        mockWebServer.shutdown()
    }

    @Test
    fun `no network connection throws an error`() {
        Mockito.`when`(mockNetworkCapabilities.hasTransport(anyInt())).thenReturn(false)

        val testDataJson = "{\"name\":\"test\"}"
        val successResponse = MockResponse().setBody(testDataJson)
        val api = retrofit.create(TestApi::class.java)
        mockWebServer.enqueue(successResponse)
        assertThrows<IOException> {
            api.test().execute()
        }
    }

    @Test
    fun `network connection does not throw an error`() {
        Mockito.`when`(mockNetworkCapabilities.hasTransport(anyInt())).thenReturn(true)

        val testDataJson = "{\"name\":\"test\"}"
        val successResponse = MockResponse().setBody(testDataJson)
        val api = retrofit.create(TestApi::class.java)
        mockWebServer.enqueue(successResponse)
        assertDoesNotThrow {
            api.test().execute()
        }
    }

    private interface TestApi {

        @GET("/test")
        fun test(): Call<TestData>
    }

    private data class TestData(val name: String)
}
