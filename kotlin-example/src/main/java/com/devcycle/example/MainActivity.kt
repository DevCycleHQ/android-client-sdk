package com.devcycle.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.api.DVCClient
import com.devcycle.sdk.android.model.DVCEvent
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.LogLevel

class MainActivity : AppCompatActivity() {

    private fun setTextValue(value: String?) {
        findViewById<TextView>(R.id.connection_status).text = "Realtime Variable Value is: " + value

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val client = DevCycleManager.dvcClient.get()

        if (client != null) {
            val variable = client.variable("realtime-demo", "default")
            setTextValue(variable.value)

            variable.onUpdate(object: DVCCallback<Variable<String>> {
                override fun onSuccess(result: Variable<String>) {
                    setTextValue(variable.value)
                }

                override fun onError(t: Throwable) {
                    // no-op
                }
            })
        }
    }
}