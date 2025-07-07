package com.devcycle.sdk.android.openfeature

import android.content.Context
import com.devcycle.sdk.android.api.DevCycleCallback
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.api.DevCycleOptions
import com.devcycle.sdk.android.model.BaseConfigVariable
import com.devcycle.sdk.android.model.DevCycleEvent
import com.devcycle.sdk.android.model.DevCycleUser
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
            DevCycleLogger.d("DevCycle OpenFeature provider onContextSet: $newContext")
            
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
                            DevCycleLogger.d("DevCycle user updated successfully via OpenFeature context")
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
        DevCycleLogger.d("DevCycle OpenFeature provider shutdown")
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?
    ): ProviderEvaluation<Boolean> {
        val client = devCycleClient
        if (client == null) {
            return ProviderEvaluation(
                value = defaultValue,
                reason = "DEFAULT"
            )
        }

        val variable = client.variable(key, defaultValue)
        return ProviderEvaluation(
            value = variable.value,
            variant = variable.key,
            reason = if (variable.isDefaulted == true) "DEFAULT" else "TARGETING_MATCH"
        )
    }

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?
    ): ProviderEvaluation<Double> {
        val client = devCycleClient
        if (client == null) {
            return ProviderEvaluation(
                value = defaultValue,
                reason = "DEFAULT"
            )
        }

        val variable = client.variable(key, defaultValue as Number)
        val doubleValue = variable.value.toDouble()
        return ProviderEvaluation(
            value = doubleValue,
            variant = variable.key,
            reason = if (variable.isDefaulted == true) "DEFAULT" else "TARGETING_MATCH"
        )
    }

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?
    ): ProviderEvaluation<Int> {
        val client = devCycleClient
        if (client == null) {
            return ProviderEvaluation(
                value = defaultValue,
                reason = "DEFAULT"
            )
        }

        val variable = client.variable(key, defaultValue as Number)
        val intValue = variable.value.toInt()
        return ProviderEvaluation(
            value = intValue,
            variant = variable.key,
            reason = if (variable.isDefaulted == true) "DEFAULT" else "TARGETING_MATCH"
        )
    }

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?
    ): ProviderEvaluation<Value> {
        val client = devCycleClient
        if (client == null) {
            return ProviderEvaluation(
                value = defaultValue,
                reason = "DEFAULT"
            )
        }

        val result = when {
            defaultValue is Value.Structure -> {
                val variable = client.variable(key, JSONObject())
                if (variable.isDefaulted == true) {
                    defaultValue
                } else {
                    Value.Structure(convertJsonObjectToMap(variable.value))
                }
            }
            defaultValue is Value.List -> {
                val variable = client.variable(key, JSONArray())
                if (variable.isDefaulted == true) {
                    defaultValue
                } else {
                    Value.List(convertJsonArrayToList(variable.value))
                }
            }
            else -> {
                val variable = client.variable(key, defaultValue.asString() ?: "")
                if (variable.isDefaulted == true) {
                    defaultValue
                } else {
                    Value.String(variable.value)
                }
            }
        }

        val isDefaulted = when {
            defaultValue is Value.Structure -> client.variable(key, JSONObject()).isDefaulted == true
            defaultValue is Value.List -> client.variable(key, JSONArray()).isDefaulted == true
            else -> client.variable(key, defaultValue.asString() ?: "").isDefaulted == true
        }

        return ProviderEvaluation(
            value = result,
            variant = key,
            reason = if (isDefaulted) "DEFAULT" else "TARGETING_MATCH"
        )
    }

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?
    ): ProviderEvaluation<String> {
        val client = devCycleClient
        if (client == null) {
            return ProviderEvaluation(
                value = defaultValue,
                reason = "DEFAULT"
            )
        }

        val variable = client.variable(key, defaultValue)
        return ProviderEvaluation(
            value = variable.value,
            variant = variable.key,
            reason = if (variable.isDefaulted == true) "DEFAULT" else "TARGETING_MATCH"
        )
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
            val event = DevCycleEvent.builder()
                .withType(trackingEventName)
                .apply {
                    details?.value?.let { value ->
                        when (value) {
                            is Number -> withValue(BigDecimal.valueOf(value.toDouble()))
                            else -> DevCycleLogger.w("DevCycle events only support numeric values, ignoring non-numeric value")
                        }
                    }
                    details?.structure?.asMap()?.let { detailData ->
                        val metadata = mutableMapOf<String, Any>()
                        if (detailData is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            metadata.putAll(detailData as Map<String, Any>)
                        }
                        if (metadata.isNotEmpty()) {
                            withMetaData(metadata)
                        }
                    }
                }
                .build()

            client.track(event)
            DevCycleLogger.d("Tracked event '$trackingEventName' via OpenFeature")
        } catch (e: Exception) {
            DevCycleLogger.e("Error tracking event '$trackingEventName': ${e.message}")
        }
    }

    private fun convertJsonObjectToMap(jsonObject: JSONObject): Map<String, Value> {
        val map = mutableMapOf<String, Value>()
        jsonObject.keys().forEach { key ->
            val value = jsonObject.get(key)
            map[key] = convertToValue(value)
        }
        return map
    }

    private fun convertJsonArrayToList(jsonArray: JSONArray): List<Value> {
        val list = mutableListOf<Value>()
        for (i in 0 until jsonArray.length()) {
            val value = jsonArray.get(i)
            list.add(convertToValue(value))
        }
        return list
    }

    private fun convertToValue(value: Any?): Value = when (value) {
        is Boolean -> Value.Boolean(value)
        is Int -> Value.Integer(value)
        is Double -> Value.Double(value)
        is String -> Value.String(value)
        is JSONObject -> Value.Structure(convertJsonObjectToMap(value))
        is JSONArray -> Value.List(convertJsonArrayToList(value))
        null -> Value.String("")
        else -> Value.String(value.toString())
    }
} 