package com.devcycle.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.api.DVCClient
import com.devcycle.sdk.android.model.DVCEvent
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.LogLevel

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    var variable: Variable<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client: DVCClient = DVCClient.builder()
            .withContext(applicationContext)
            .withUser(
                DVCUser.builder()
                    .withUserId("nic_test")
                    .build()
            )
            .withEnvironmentKey("mobile-c06ea707-791d-4978-a91d-c863616b7f51")
            .withLogLevel(LogLevel.DEBUG)
            .build()

        variable = client.variable("activate-flag", "not activated")
        Toast.makeText(this@MainActivity, variable?.value, Toast.LENGTH_SHORT).show()

        client.onInitialized(object : DVCCallback<String> {
            override fun onSuccess(result: String) {
                //Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()

                client.flushEvents(object: DVCCallback<String> {
                    override fun onSuccess(result: String) {
                        Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(t: Throwable) {
                        Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
                })

                client.track(DVCEvent.builder()
                    .withType("testEvent")
                    .withMetaData(mapOf("test" to "value"))
                    .build())

                //this@MainActivity.runOnUiThread {
                    Toast.makeText(this@MainActivity, variable?.value, Toast.LENGTH_SHORT).show()
                //}
            }

            override fun onError(t: Throwable) {
                //this@MainActivity.runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: " + t.message, Toast.LENGTH_SHORT)
                        .show()
                //}
            }
        })
    }
}