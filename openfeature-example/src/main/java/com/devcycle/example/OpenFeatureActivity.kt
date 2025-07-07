package com.devcycle.example

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.devcycle.openfeatureexample.R
import dev.openfeature.sdk.*

class OpenFeatureActivity : AppCompatActivity() {

    private fun setTextValue(value: String?) {
        findViewById<TextView>(R.id.connection_status).text = "OpenFeature Flag Value: $value"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Get client directly from OpenFeature API
        val client = OpenFeatureAPI.getClient()
        
        // Get flag values - OpenFeature handles provider state internally
        val stringFlag = client.getStringValue("realtime-demo", "default string")
        val booleanFlag = client.getBooleanValue("boolean-flag", false)
        val integerFlag = client.getIntegerValue("integer-flag", 42)
        val doubleFlag = client.getDoubleValue("double-flag", 3.14)
        
        // Display all flag values
        val allFlags = """
            String Flag: $stringFlag
            Boolean Flag: $booleanFlag
            Integer Flag: $integerFlag
            Double Flag: $doubleFlag
        """.trimIndent()
        
        setTextValue(allFlags)
        
        // Track a page view event
        client.track(
            "page_view",
            TrackingEventDetails(
                structure = ImmutableStructure(mapOf(
                    "page" to Value.String("OpenFeatureActivity")
                ))
            )
        )
    }
}