package com.devcycle.example

import android.app.Application
import android.widget.Toast
import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.api.DVCClient
import com.devcycle.sdk.android.model.DVCEvent
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.LogLevel
import timber.log.Timber


class KotlinApplication: Application() {
    var variable: Variable<String>? = null

    override fun onCreate() {
        super.onCreate()
        val client: DVCClient = DVCClient.builder()
            .withContext(applicationContext)
            .withUser(
                DVCUser.builder()
                    .withUserId("test_user")
//                    .withIsAnonymous(true)
                    .withCustomData(mapOf("custom_value" to "test"))
                    .build()
            )
            .withEnvironmentKey("asdsa")
            .withLogLevel(LogLevel.DEBUG)
            .withLogger(Timber.DebugTree())
            .build()

        // Use your own demo variable here to see the value change from the defaultValue when the client is initialized
        variable = client.variable("<YOUR_VARIABLE_KEY>", "my string variable is not initialized yet");

        client.onInitialized(object : DVCCallback<String> {
            override fun onSuccess(result: String) {
//                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                client.flushEvents(object: DVCCallback<String> {
                    override fun onSuccess(result: String) {
                        Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(t: Throwable) {
                        Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    }
                })

                client.track(
                    DVCEvent.builder()
                    .withType("testEvent")
                    .withMetaData(mapOf("test" to "value"))
                    .build())


                // This toast onInitialized will show the value has changed
                Toast.makeText(applicationContext, "$variable?.value", Toast.LENGTH_SHORT).show()
            }

            override fun onError(t: Throwable) {
                Toast.makeText(applicationContext, "Client did not initialize: " + t.message, Toast.LENGTH_SHORT).show()
            }
        })

        DevCycleManager.setClient(client)
    }
}