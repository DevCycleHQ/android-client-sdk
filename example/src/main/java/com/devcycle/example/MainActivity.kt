package com.devcycle.example

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devcycle.android.client.sdk.api.DVCCallback
import com.devcycle.android.client.sdk.api.DVCClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client: DVCClient = DVCClient.builder()
            .withContext(applicationContext)
            .withUser(
                com.devcycle.android.client.sdk.model.User.Companion.builder()
                    .withUserId("j_test")
                    .withIsAnonymous(false)
                    .build()
            )
            .withEnvironmentKey("add-client-sdk")
            .build()

        client.initialize(object : DVCCallback<String?> {
            override fun onSuccess(result: String?) {
                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
            }

            override fun onError(t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: " + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}