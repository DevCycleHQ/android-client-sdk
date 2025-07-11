package com.devcycle.example

import android.app.Application
import android.widget.Toast
import com.devcycle.sdk.android.api.DevCycleCallback
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.model.DevCycleEvent
import com.devcycle.sdk.android.model.DevCycleUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.DevCycleLogger
import com.devcycle.sdk.android.util.LogLevel
import java.util.Objects

class KotlinApplication: Application() {
    var variable: Variable<String>? = null
    var variableValue: String? = null

    override fun onCreate() {
        super.onCreate()
        val client: DevCycleClient = DevCycleClient.builder()
            .withContext(applicationContext)
            .withUser(
                DevCycleUser.builder()
                    .withUserId("test_user")
                    .withCustomData(mapOf("custom_value" to "test"))
                    .build()
            )
            .withSDKKey("<DEVCYCLE_MOBILE_SDK_KEY>")
            .logLevel(LogLevel.DEBUG)
            .withLogger(DevCycleLogger.DebugLogger())
            .build()

        // Use your own demo variable here to see the value change from the defaultValue when the client is initialized
        variable = client.variable("<YOUR_VARIABLE_KEY>", "my string variable is not initialized yet");
        variableValue = client.variableValue("<YOUR_VARIABLE_KEY>", "default value")
        Toast.makeText(applicationContext, Objects.requireNonNull(variableValue), Toast.LENGTH_SHORT).show()

        client.onInitialized(object : DevCycleCallback<String> {
            override fun onSuccess(result: String) {
//                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                client.flushEvents(object: DevCycleCallback<String> {
                    override fun onSuccess(result: String) {
                        Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(t: Throwable) {
                        Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    }
                })

                client.track(
                    DevCycleEvent.builder()
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