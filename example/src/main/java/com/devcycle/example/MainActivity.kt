package com.devcycle.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.api.DVCClient
import com.devcycle.sdk.android.model.DVCEvent
import com.devcycle.sdk.android.model.DVCResponse
import com.devcycle.sdk.android.model.DVCUser

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client: DVCClient = DVCClient.builder()
            .withContext(applicationContext)
            .withUser(
                DVCUser(
                    userId = "user_test",
                    isAnonymous = false
                )
            )
            .withEnvironmentKey("add-client-sdk")
            .build()

        client.initialize(object : DVCCallback<String?> {
            override fun onSuccess(result: String?) {
                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()

                client.track(DVCEvent(
                    type = "testEvent",
                    metaData = mapOf("test" to "value")
                ), object: DVCCallback<String?> {
                    override fun onSuccess(result: String?) {
                        Log.i(TAG, result ?: "Success Event")
                    }

                    override fun onError(t: Throwable) {
                        Log.e(TAG, "Failed Event", t)
                    }
                })
            }

            override fun onError(t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: " + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}