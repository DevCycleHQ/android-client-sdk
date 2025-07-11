package com.devcycle.example

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.devcycle.openfeatureexample.R
import dev.openfeature.sdk.*

class OpenFeatureActivity : AppCompatActivity() {

    private lateinit var featureFlagKeyEditText: EditText
    private lateinit var userKeyEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var resultTextView: TextView
    private lateinit var offlineSwitch: Switch

    private fun setTextValue(value: String?) {
        findViewById<TextView>(R.id.connection_status).text = "OpenFeature Flag Value: $value"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupSpinner()
        setupButtonListeners()
        
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
    
    private fun initializeViews() {
        featureFlagKeyEditText = findViewById(R.id.feature_flag_key)
        userKeyEditText = findViewById(R.id.userKey_editText)
        typeSpinner = findViewById(R.id.type_spinner)
        resultTextView = findViewById(R.id.result_textView)
        offlineSwitch = findViewById(R.id.offlineSwitch)
    }
    
    private fun setupSpinner() {
        val types = arrayOf("String", "Boolean", "Integer", "Double")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
    }
    
    private fun setupButtonListeners() {
        findViewById<Button>(R.id.identify_button).setOnClickListener {
            identifyUser()
        }
        
        findViewById<Button>(R.id.track_button).setOnClickListener {
            trackEvent()
        }
    }
    
    private fun identifyUser() {
        val userKey = userKeyEditText.text.toString()
        // TODO: Implement user identification logic
        Toast.makeText(this, "Identify user: $userKey", Toast.LENGTH_SHORT).show()

        val evaluationContext = ImmutableContext(
            targetingKey = userKey
        )
        
        // Set the evaluation context globally
        OpenFeatureAPI.setEvaluationContext(evaluationContext)
    }
    
    private fun trackEvent() {
        val client = OpenFeatureAPI.getClient()
        client.track("track_event")
    }
}