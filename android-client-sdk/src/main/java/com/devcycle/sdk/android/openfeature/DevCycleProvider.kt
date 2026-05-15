package com.devcycle.sdk.android.openfeature

import android.content.Context
import com.devcycle.sdk.android.api.DevCycleCallback
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.api.DevCycleOptions
import com.devcycle.sdk.android.model.BaseConfigVariable
import com.devcycle.sdk.android.model.DevCycleUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.DevCycleLogger
import dev.openfeature.kotlin.sdk.*
import dev.openfeature.kotlin.sdk.events.OpenFeatureProviderEvents
import dev.openfeature.kotlin.sdk.exceptions.OpenFeatureError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DevCycleProvider(
    private val sdkKey: String,
    private val context: Context,
    private val options: DevCycleOptions? = null,
    override val hooks: List<Hook<*>> = emptyList(),
    override val metadata: ProviderMetadata = DevCycleProviderMetadata()
) : FeatureProvider {

    private val _providerEvents = MutableSharedFlow<OpenFeatureProviderEvents>(extraBufferCapacity = 1)

    /**
     * The DevCycle client instance - created during initialization
     */
    private var _devcycleClient: DevCycleClient? = null

    val devcycleClient: DevCycleClient
        get() = _devcycleClient
            ?: error(
                """
                DevCycleClient is not initialized. Call OpenFeatureAPI.setProvider() / OpenFeatureAPI.setProviderAndWait() with this provider instance to initialize the DevCycleClient.
                """.trimIndent()
            )

    /**
     * [hasUsableCachedConfig] must be captured from the same client reference used to retrieve
     * [variable] so the reason reflects the config state at evaluation time rather than a later
     * read of the mutable [_devcycleClient] field.
     */
    private fun <T> createProviderEvaluation(
        variable: Variable<*>,
        value: T,
        hasUsableCachedConfig: Boolean,
    ): ProviderEvaluation<T> {
        val metadataBuilder = EvaluationMetadata.builder()
        var hasMetadata = false

        variable.eval?.let { evalReason ->
            evalReason.details?.let { details ->
                metadataBuilder.putString("evalDetails", details)
                hasMetadata = true
            }
            evalReason.targetId?.let { targetId ->
                metadataBuilder.putString("evalTargetId", targetId)
                hasMetadata = true
            }
        }

        val reason = when {
            variable.isDefaulted == true -> Reason.DEFAULT.toString()
            hasUsableCachedConfig -> Reason.CACHED.toString()
            else -> variable.eval?.reason ?: Reason.TARGETING_MATCH.toString()
        }

        return ProviderEvaluation(
            value = value,
            variant = variable.key,
            reason = reason,
            metadata = if (hasMetadata) metadataBuilder.build() else EvaluationMetadata.EMPTY
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

            _devcycleClient = clientBuilder.build()

            // consumeResume guard — ensures continuation.resume is called exactly once.
            // On a cache hit the cache check below resolves immediately; onInitialized fires later
            // (after network) and emits ProviderConfigurationChanged.
            // On a cache miss onInitialized is the sole resolve.
            val lock = Any()
            var didResume = false
            fun consumeResume(): Boolean = synchronized(lock) {
                if (didResume) false else { didResume = true; true }
            }

            // Captured before registering callbacks to avoid a race where onInitialized fires
            // first and incorrectly treats a cache-hit init as a fatal error.
            val isCacheHit = _devcycleClient!!.hasUsableCachedConfig

            suspendCancellableCoroutine<Unit> { continuation ->

                // SSE-triggered config updates fire post-init whenever the realtime connection
                // delivers a new config.
                _devcycleClient!!.onConfigUpdated(object : DevCycleCallback<Map<String, BaseConfigVariable>> {
                    override fun onSuccess(result: Map<String, BaseConfigVariable>) {
                        _providerEvents.tryEmit(OpenFeatureProviderEvents.ProviderConfigurationChanged)
                    }
                    override fun onError(t: Throwable) {
                        _providerEvents.tryEmit(OpenFeatureProviderEvents.ProviderError(
                            OpenFeatureError.GeneralError(t.message ?: "Config error")
                        ))
                    }
                })

                // onInitialized always fires after the network fetch completes.
                // Cache miss: sole resolve — resume the continuation.
                // Cache hit: continuation already resumed below; surface the network result as an event.
                _devcycleClient!!.onInitialized(object : DevCycleCallback<String> {
                    override fun onSuccess(result: String) {
                        if (!continuation.isActive) return
                        if (consumeResume()) {
                            continuation.resume(Unit)
                        } else {
                            _providerEvents.tryEmit(OpenFeatureProviderEvents.ProviderConfigurationChanged)
                        }
                    }
                    override fun onError(t: Throwable) {
                        if (!continuation.isActive) return
                        if (!isCacheHit) {
                            // No cache — fatal.
                            if (consumeResume()) {
                                continuation.resumeWithException(
                                    OpenFeatureError.ProviderFatalError("DevCycle client initialization error: ${t.message}")
                                )
                            }
                        } else {
                            // Cache hit: network error is non-fatal; cached config remains usable.
                            _providerEvents.tryEmit(OpenFeatureProviderEvents.ProviderError(
                                OpenFeatureError.GeneralError("Background refresh failed: ${t.message}")
                            ))
                        }
                    }
                })

                // Cache hit: resolve immediately — the network fetch continues in the background.
                if (isCacheHit) {
                    if (continuation.isActive && consumeResume()) {
                        continuation.resume(Unit)
                    }
                }
            }
        } catch (e: OpenFeatureError) {
            // Re-throw OpenFeature errors as-is
            throw e
        } catch (e: Exception) {
            throw OpenFeatureError.ProviderFatalError("DevCycle client initialization error: ${e.message}")
        }
    }

    override suspend fun onContextSet(oldContext: EvaluationContext?, newContext: EvaluationContext) {
        try {
            val client = _devcycleClient
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

    override fun observe(): Flow<OpenFeatureProviderEvents> = _providerEvents.asSharedFlow()

    override fun shutdown() {
        _devcycleClient?.close()
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?
    ): ProviderEvaluation<Boolean> {
        val client = _devcycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value, client.hasUsableCachedConfig)
    }

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?
    ): ProviderEvaluation<Double> {
        val client = _devcycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value.toDouble(), client.hasUsableCachedConfig)
    }

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?
    ): ProviderEvaluation<Int> {
        val client = _devcycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value.toInt(), client.hasUsableCachedConfig)
    }

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?
    ): ProviderEvaluation<Value> {
        val client = _devcycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val hasUsableCachedConfig = client.hasUsableCachedConfig

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

        return createProviderEvaluation(variable, result, hasUsableCachedConfig)
    }

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?
    ): ProviderEvaluation<String> {
        val client = _devcycleClient ?: return createDefaultProviderEvaluation(defaultValue)
        val variable = client.variable(key, defaultValue)
        return createProviderEvaluation(variable, variable.value, client.hasUsableCachedConfig)
    }

    override fun track(
        trackingEventName: String,
        context: EvaluationContext?,
        details: TrackingEventDetails?
    ) {
        val client = _devcycleClient
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