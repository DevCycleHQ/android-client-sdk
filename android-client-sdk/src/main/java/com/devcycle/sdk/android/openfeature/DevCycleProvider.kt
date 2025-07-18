package com.devcycle.sdk.android.openfeature

import android.content.Context
import com.devcycle.sdk.android.api.DevCycleCallback
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.api.DevCycleOptions
import com.devcycle.sdk.android.model.BaseConfigVariable
import com.devcycle.sdk.android.model.DevCycleEvent
import com.devcycle.sdk.android.model.DevCycleUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.DevCycleLogger
import dev.openfeature.sdk.*
import dev.openfeature.sdk.exceptions.OpenFeatureError
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DevCycleProvider(
    private val sdkKey: String,
    private val context: Context,
    private val options: DevCycleOptions? = null,
    override val hooks: List<Hook<*>> = emptyList(),
    override val metadata: ProviderMetadata = DevCycleProviderMetadata()
) : FeatureProvider {

    /**
     * The DevCycle client instance - created during initialization
     */
    private var devCycleClient: DevCycleClient? = null

    /**
     * Helper function to create a ProviderEvaluation from a DevCycle variable
     */
    private fun <T> createProviderEvaluation(variable: Variable<*>, value: T): ProviderEvaluation<T> {
        val metadataBuilder = EvaluationMetadata.builder()
        
        // Add evaluation details and target ID if available
        variable.eval?.let { evalReason ->
            evalReason.details?.let { details ->
                metadataBuilder.putString("evalDetails", details)
            }
            evalReason.targetId?.let { targetId ->
                metadataBuilder.putString("evalTargetId", targetId)
            }
        }
        
        return ProviderEvaluation(
            value = value,
            variant = variable.key,
            reason = variable.eval?.reason ?: if (variable.isDefaulted == true) Reason.DEFAULT.toString() else Reason.TARGETING_MATCH.toString(),
            metadata = metadataBuilder.build()
        )
    }

    /**
     * Helper function to create a default ProviderEvaluation when client is not available
     */
    private fun <T> createDefaultProviderEvaluation(defaultValue: T): ProviderEvaluation<T> {
        return ProviderEvaluation(
            value = defaultValue,
            reason = Reason.DEFAULT.toString()
        )
    }

    override suspend fun initialize(initialContext: EvaluationContext?) {
        if (initialContext == null) {
            DevCycleLogger.w(
                "DevCycleProvider initialized without context being set. " +
                "It is highly recommended to set a context using OpenFeature.setContext() " +
                "before setting an OpenFeature Provider using OpenFeature.setProvider() " +
                "to avoid multiple API fetch calls."
            )
        }

        try {
            // If initialContext is null, use anonymous user
            // Otherwise, convert context to user and throw any errors
            val user: DevCycleUser = if (initialContext != null) {
                DevCycleContextMapper.evaluationContextToDevCycleUser(initialContext)
                    ?: throw OpenFeatureError.InvalidContextError("Invalid context provided for DevCycle user creation")
            } else {
                DevCycleUser.builder().withIsAnonymous(true).build() // Anonymous user
            }

            // Initialize DevCycle client
            val clientBuilder = DevCycleClient.builder()
                .withContext(context)
                .withSDKKey(sdkKey)
                .withUser(user)
            
            options?.let { clientBuilder.withOptions(it) }
            
            devCycleClient = clientBuilder.build()

            // Wait for DevCycle client to fully initialize
            suspendCancellableCoroutine<Unit> { continuation ->
                devCycleClient!!.onInitialized(object : DevCycleCallback<String> {
                    override fun onSuccess(result: String) {
                        DevCycleLogger.d("DevCycle OpenFeature provider initialized successfully")
                        continuation.resume(Unit)
                    }

                    override fun onError(t: Throwable) {
                        DevCycleLogger.e("DevCycle OpenFeature provider initialization failed: ${t.message}")
                        continuation.resumeWithException(
                            OpenFeatureError.ProviderFatalError("DevCycle client initialization error: ${t.message}")
                        )
                    }
                })
            }
        } catch (e: OpenFeatureError) {
            // Re-throw OpenFeature errors as-is
            throw e
        } catch (e: Exception) {
            DevCycleLogger.e("DevCycle OpenFeature provider initialization failed: ${e.message}")
            throw OpenFeatureError.ProviderFatalError("DevCycle client initialization error: ${e.message}")
        }
    }

    override suspend fun onContextSet(oldContext: EvaluationContext?, newContext: EvaluationContext) {
        try {            
            val client = devCycleClient
            if (client == null) {
                DevCycleLogger.w(
                    "Context set before DevCycleProvider was fully initialized. " +
                    "The context will be ignored until initialization completes."
                )
                return
            }

            val devCycleUser = DevCycleContextMapper.evaluationContextToDevCycleUser(newContext)
            devCycleUser?.let { user ->
                // Use suspendCancellableCoroutine to wait for identifyUser to complete
                suspendCancellableCoroutine<Unit> { continuation ->
                    client.identifyUser(user, object : DevCycleCallback<Map<String, BaseConfigVariable>> {
                        override fun onSuccess(result: Map<String, BaseConfigVariable>) {
                            continuation.resume(Unit)
                        }

                        override fun onError(t: Throwable) {
                            DevCycleLogger.e("Error updating DevCycle user via OpenFeature context: ${t.message}")
                            continuation.resumeWithException(t)
                        }
                    })
                }
            }
        } catch (e: Exception) {
            DevCycleLogger.e("Error processing OpenFeature context change: ${e.message}")
            throw OpenFeatureError.GeneralError("Error setting context: ${e.message}")
        }
    }

    override fun shutdown() {
        devCycleClient?.close()
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?
    ): ProviderEvaluation<Boolean> {
        val client = devCycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value)
    }

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?
    ): ProviderEvaluation<Double> {
        val client = devCycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value.toDouble())
    }

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?
    ): ProviderEvaluation<Int> {
        val client = devCycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value.toInt())
    }

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?
    ): ProviderEvaluation<Value> {
        val client = devCycleClient ?: return createDefaultProviderEvaluation(defaultValue)

        val (result, variable) = when {
            defaultValue is Value.Structure -> {
                val variable = client.variable(key, JSONObject())
                val value = if (variable.isDefaulted == true) {
                    defaultValue
                } else {
                    Value.Structure(JsonValueConverter.convertJsonObjectToMap(variable.value))
                }
                Pair(value, variable)
            }
            defaultValue is Value.List -> {
                val variable = client.variable(key, JSONArray())
                val value = if (variable.isDefaulted == true) {
                    defaultValue
                } else {
                    Value.List(JsonValueConverter.convertJsonArrayToList(variable.value))
                }
                Pair(value, variable)
            }
            else -> {
                val variable = client.variable(key, defaultValue.asString() ?: "")
                val value = if (variable.isDefaulted == true) {
                    defaultValue
                } else {
                    Value.String(variable.value)
                }
                Pair(value, variable)
            }
        }

        return createProviderEvaluation(variable, result)
    }

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?
    ): ProviderEvaluation<String> {
        val client = devCycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value)
    }

    override fun track(
        trackingEventName: String,
        context: EvaluationContext?,
        details: TrackingEventDetails?
    ) {
        val client = devCycleClient
        if (client == null) {
            DevCycleLogger.w("Cannot track event '$trackingEventName': DevCycle client not initialized")
            return
        }

        try {
            val event = DevCycleEventMapper.openFeatureEventToDevCycleEvent(trackingEventName, details)
            client.track(event)
            DevCycleLogger.d("Tracked event '$trackingEventName' via OpenFeature")
        } catch (e: Exception) {
            DevCycleLogger.e("Error tracking event '$trackingEventName': ${e.message}")
        }
    }

} 