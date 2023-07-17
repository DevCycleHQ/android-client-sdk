package com.devcycle.example

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private fun setTextValue(value: String?) {
        findViewById<TextView>(R.id.connection_status).text = "Realtime Variable Value is: " + value

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val client = DevCycleManager.devCycleClient.get()

        if (client != null) {
            val variable = client.variable("realtime-demo", "default")
            setTextValue(variable.value)
            variable.onUpdate {
                setTextValue(it.value)
            }
        }
    }
}