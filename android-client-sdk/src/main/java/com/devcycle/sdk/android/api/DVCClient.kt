package com.devcycle.sdk.android.api

import android.content.Context
import android.util.Log
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.DVCSharedPrefs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main entry point for SDK user
 * The class is constructed by calling DVCClient.builder(){builder options}.build()
 * [initialize] must be called to properly initialize the client and retrieve
 * the configuration.
 *
 * All methods that make requests to the APIs or access [config] and [user] are [Synchronized] to
 * ensure thread-safety
 */
class DVCClient private constructor(
    context: Context,
    private val environmentKey: String,
    private var user: User,
    private var options: DVCOptions?,
) {
    private val TAG: String = DVCClient::class.java.simpleName

    internal var config: BucketedUserConfig? = null
    private val defaultIntervalInMs: Long = 10000
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context)
    private val request: Request = Request(environmentKey)
    private val observable: BucketedUserConfigListener = BucketedUserConfigListener()
    private val eventQueue: EventQueue = EventQueue(request, ::user)

    private val isInitialized = AtomicBoolean(false)
    private val isExecuting = AtomicBoolean(false)

    private val timer: Timer = Timer("DevCycle.EventQueue.Timer", true)

    private val coroutineScope = MainScope()

    @Synchronized
    fun initialize(callback: DVCCallback<String?>) {
        if (isExecuting.get()) {
            callback.onError(IllegalStateException("DVCClient already initializing"))
            return
        }

        isExecuting.set(true)

        if (isInitialized.get()) {
            isExecuting.set(false)
            callback.onError(IllegalStateException("DVCClient already initialized"))
            return
        }

        val now = System.currentTimeMillis()
        fetchConfig(user, object : DVCCallback<BucketedUserConfig> {
            override fun onSuccess(result: BucketedUserConfig) {
                isInitialized.set(true)
                isExecuting.set(false)
                initializeEventQueue()

                eventQueue.queueEvent(
                    Event.fromInternalEvent(
                        Event.userConfigEvent(
                            BigDecimal(System.currentTimeMillis() - now)
                        ),
                        user,
                        result.featureVariationMap
                    )
                )

                callback.onSuccess("Config loaded")
            }

            override fun onError(t: Throwable) {
                isExecuting.set(false)
                callback.onError(t)
            }
        })
    }

    /**
     * Updates or builds a new User and fetches the latest config for that User
     *
     * [user] is a lightweight User object that can identify and update the current User or will
     * build a new one.
     * [callback] is provided by the SDK user and will callback with the Map of Variables in the
     * latest config when fetched from the API
     */
    @Synchronized
    fun identifyUser(user: DVCUser, callback: DVCCallback<Map<String, Variable<Any>>>? = null) {
        var updatedUser: User
        if (this.user.userId == user.userId) {
            updatedUser = this.user.updateUser(user)
        } else {
            updatedUser = User.builder().withUserParam(user).build()
        }

        val self = this
        fetchConfig(updatedUser, object : DVCCallback<BucketedUserConfig> {
            override fun onSuccess(result: BucketedUserConfig) {
                self.user = updatedUser
                saveUser()
                config?.variables?.let { callback?.onSuccess(it) }
            }

            override fun onError(t: Throwable) {
                callback?.onError(t)
            }
        })
    }

    /**
     * Uses or builds a new Anonymous User and fetches the latest config
     *
     * [callback] is provided by the SDK user and will callback with the Map of Variables in the
     * latest config when fetched from the API
     */
    @Synchronized
    fun resetUser(callback: DVCCallback<Map<String, Variable<Any>>>? = null) {
        val newUser: User = User.builder()
                    .build()

        val self = this
        fetchConfig(newUser, object : DVCCallback<BucketedUserConfig> {
            override fun onSuccess(result: BucketedUserConfig) {
                self.user = newUser
                saveUser()
                config?.variables?.let { callback?.onSuccess(it) }
            }

            override fun onError(t: Throwable) {
                callback?.onError(t)
            }
        })
    }

    /**
     * Returns the Map of Features in the config
     */
    @Synchronized
    fun allFeatures(): Map<String, Feature>? {
        return if (config == null) emptyMap() else config!!.features
    }

    /**
     * Returns the Map of Variables in the config
     */
    @Synchronized
    fun allVariables(): Map<String, Variable<Any>>? {
        return if (config == null) emptyMap() else config!!.variables
    }

    /**
     * Retrieve a Variable from the config. Update the Variable whenever the Config is updated using
     * [java.beans.PropertyChangeListener]
     *
     * [key] is used to identify the Variable in the config
     * [defaultValue] is set on the Variable and used to provide a default value if the Variable
     * could not be fetched or does not exist
     */
    @Synchronized
    fun <T: Any> variable(key: String, defaultValue: T): Variable<T> {
        Variable.validateType(defaultValue)
        val variableByKey: Variable<Any>? = config?.variables?.get(key)
        val variable = Variable.initializeFromVariable(key, defaultValue, variableByKey)

        observable.addPropertyChangeListener(variable)

        val tmpConfig = config
        val event: Event = Event.fromInternalEvent(
            Event.variableEvent(variable.isDefaulted, variable.key),
            user,
            tmpConfig?.featureVariationMap
        )

        try {
            eventQueue.queueAggregateEvent(event)
        } catch(e: IllegalArgumentException) {
            e.message?.let { Log.e(TAG, it) }
        }


        return variable
    }

    /**
     * Track a custom event for the current user. Requires the SDK to have finished initializing.
     *
     * [event] instance of an event object to submit
     */
    fun track(event: DVCEvent) {
        eventQueue.queueEvent(Event.fromDVCEvent(event, user, config?.featureVariationMap))
    }

    /**
     * Manually send all queued events to the API.
     *
     * [callback] optional callback to be notified on success or failure
     */
    fun flushEvents(callback: DVCCallback<String>? = null) {
        coroutineScope.launch {
            try {
                eventQueue.flushEvents()
                callback?.onSuccess("")
            } catch (t: Throwable) {
                callback?.onError(t)
            }
        }
    }

    private fun initializeEventQueue() {
        val flushInMs: Long = options?.flushEventsIntervalMs ?: defaultIntervalInMs
        timer.schedule(eventQueue, flushInMs, flushInMs)
    }

    private fun saveUser() {
        dvcSharedPrefs.save(user, DVCSharedPrefs.UserKey)
    }

    private fun fetchConfig(user: User, callback: DVCCallback<BucketedUserConfig>) {
        coroutineScope.launch {
            try {
                val result = request.getConfigJson(environmentKey, user)
                config = result
                observable.configUpdated(result)
                dvcSharedPrefs.save(config, DVCSharedPrefs.ConfigKey)
                callback.onSuccess(result)
            } catch (t: Throwable) {
                callback.onError(t)
            }
        }
    }

    class DVCClientBuilder {
        private var context: Context? = null
        private var environmentKey: String? = null
        private var user: User? = null
        private var options: DVCOptions? = null
        fun withContext(context: Context): DVCClientBuilder {
            this.context = context
            return this
        }
        fun withEnvironmentKey(environmentKey: String): DVCClientBuilder {
            this.environmentKey = environmentKey
            return this
        }

        fun withUser(user: DVCUser): DVCClientBuilder {
            this.user = User.builder().withUserParam(user).build()
            return this
        }

        fun withOptions(options: DVCOptions): DVCClientBuilder {
            this.options = options
            return this
        }

        fun build(): DVCClient {
            requireNotNull(context) { "Context must be set" }
            require(!(environmentKey == null || environmentKey == "")) { "SDK key must be set" }
            requireNotNull(user) { "User must be set" }
            return DVCClient(context!!, environmentKey!!, user!!, options)
        }
    }

    companion object {
        fun builder(): DVCClientBuilder {
            return DVCClientBuilder()
        }
    }

    init {
        saveUser()
    }
}