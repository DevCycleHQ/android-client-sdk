package com.devcycle.example

import android.app.Application
import android.widget.Toast
import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.api.DVCClient
import com.devcycle.sdk.android.model.DVCEvent
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.DVCLogger
import com.devcycle.sdk.android.util.LogLevel
import java.util.Objects

class KotlinApplication: Application() {
    var variable: Variable<String>? = null
    var variableValue: String? = null

    override fun onCreate() {
        super.onCreate()
        val client: DVCClient = DVCClient.builder()
            .withContext(applicationContext)
            .withUser(
                DVCUser.builder()
                    .withUserId("test_user")
                    .withCustomData(mapOf("custom_value" to "test"))
                    .build()
            )
            .withSDKKey("<DVC_MOBILE_SDK_KEY>")
            .withLogLevel(LogLevel.DEBUG)
            .withLogger(DVCLogger.DebugLogger())
            .build()

        DevCycleManager.setClient(client)
        // Use your own demo variable here to see the value change from the defaultValue when the client is initialized
        variable = DevCycleManager.variable("string-variable", "my string variable is not initialized yet");
        variableValue = DevCycleManager.dvcClient?.variableValue("string-variable", "default value")
        Toast.makeText(applicationContext, Objects.requireNonNull(variableValue), Toast.LENGTH_SHORT).show()

        DevCycleManager.dvcClient?.onInitialized(object : DVCCallback<String> {
            override fun onSuccess(result: String) {

    //                Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                DevCycleManager.dvcClient?.flushEvents(object: DVCCallback<String> {
                    override fun onSuccess(result: String) {
                        Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(t: Throwable) {
                        Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    }
                })

                DevCycleManager.dvcClient?.track(
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

    }
}