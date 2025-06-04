package com.devcycle.example

import android.app.Application
import android.widget.Toast
import com.devcycle.sdk.android.api.DevCycleOptions
import com.devcycle.sdk.android.openfeature.DevCycleProvider
import com.devcycle.sdk.android.util.LogLevel
import dev.openfeature.sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OpenFeatureApplication: Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Create evaluation context with user information
                val evaluationContext = ImmutableContext(
                    targetingKey = "test_user",
                    attributes = mutableMapOf(
                        "email" to Value.String("test@devcycle.com"),
                        "name" to Value.String("Test User"),
                        "country" to Value.String("CA"),
                        "customData" to Value.Structure(mapOf(
                            "custom_value" to Value.String("test")
                        ))
                    )
                )
                
                // Set the evaluation context globally
                OpenFeatureAPI.setEvaluationContext(evaluationContext)
                
                // Create DevCycle provider with options
                val options = DevCycleOptions.builder()
                    .withLogLevel(LogLevel.DEBUG)
                    .build()
                    
                val provider = DevCycleProvider(
                    sdkKey = "<DEVCYCLE_MOBILE_SDK_KEY>",
                    context = applicationContext,
                    options = options
                )
                
                // Set provider and wait for initialization
                OpenFeatureAPI.setProviderAndWait(provider)
                
                // Get OpenFeature client directly from API
                val client = OpenFeatureAPI.getClient()
                
                // Evaluate a flag to test the integration
                val flagValue = client.getStringValue("<YOUR_VARIABLE_KEY>", "default value")
                
                launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "OpenFeature initialized! Flag value: $flagValue", Toast.LENGTH_SHORT).show()
                }
                
                // Track an event to test event functionality
                client.track(
                    "app_started",
                    TrackingEventDetails(
                        value = 1.0,
                        structure = ImmutableStructure(mapOf(
                            "platform" to Value.String("android"),
                            "sdk_type" to Value.String("openfeature")
                        ))
                    )
                )
                
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "OpenFeature initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}