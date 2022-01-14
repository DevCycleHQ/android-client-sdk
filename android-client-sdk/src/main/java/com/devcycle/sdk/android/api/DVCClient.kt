package com.devcycle.sdk.android.api

import android.content.Context
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.DVCSharedPrefs
import com.devcycle.sdk.android.util.LogLevel
import com.devcycle.sdk.android.util.LogTree
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.TestOnly
import timber.log.Timber
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Main entry point for SDK user
 * The class is constructed by calling DVCClient.builder(){builder options}.build()
 *
 * All methods that make requests to the APIs or access [config] and [user] are [Synchronized] to
 * ensure thread-safety
 */
class DVCClient private constructor(
    private val context: Context,
    private val environmentKey: String,
    private var user: User,
    private var options: DVCOptions?,
    logLevel: LogLevel,
    apiUrl: String,
    private val coroutineScope: CoroutineScope = MainScope(),
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) {
    private var config: BucketedUserConfig? = null

    private val defaultIntervalInMs: Long = 10000
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context)
    private val request: Request = Request(environmentKey, apiUrl)
    private val observable: BucketedUserConfigListener = BucketedUserConfigListener()
    private val eventQueue: EventQueue = EventQueue(request, ::user, CoroutineScope(coroutineContext))

    private val isInitialized = AtomicBoolean(false)
    private val isExecuting = AtomicBoolean(false)
    private val initializeJob: Deferred<Any>

    private val timer: Timer = Timer("DevCycle.EventQueue.Timer", true)

    private val configRequestQueue = ConcurrentLinkedQueue<UserAndCallback>()
    private val configRequestMutex = Mutex()

    init {
        initializeJob = coroutineScope.async(coroutineContext) {
            isExecuting.set(true)
            try {
                fetchConfig(user)
                isInitialized.set(true)
                initializeEventQueue()
            } catch (t: Throwable) {
                Timber.e(t, "DevCycle SDK Failed to Initialize!")
                throw t
            } finally {
                handleQueuedConfigRequests()
                isExecuting.set(false)
            }
        }
    }

    fun onInitialized(callback: DVCCallback<String>) {
        if (isInitialized.get()) {
            callback.onSuccess("Config loaded")
            return
        }

        coroutineScope.launch {
            try {
                initializeJob.await()
                callback.onSuccess("Config loaded")
            } catch (t: Throwable) {
                callback.onError(t)
            }
        }
    }

    /**
     * Updates or builds a new User and fetches the latest config for that User
     *
     * [user] is a lightweight User object that can identify and update the current User or will
     * build a new one.
     * [callback] is optional and provided by the SDK user and will callback with the Map of
     * Variables in the latest config when fetched from the API
     */
    @Synchronized
    fun identifyUser(user: DVCUser, callback: DVCCallback<Map<String, Variable<Any>>>? = null) {
        val updatedUser: User = if (this@DVCClient.user.userId == user.userId) {
            this@DVCClient.user.copyUserAndUpdateFromDVCUser(user)
        } else {
            User.builder().withUserParam(user, context).build()
        }

        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(updatedUser, callback))
        } else {
            flushEvents()

            coroutineScope.launch(coroutineContext) {
                isExecuting.set(true)

                try {
                    fetchConfig(updatedUser)
                    config?.variables?.let { callback?.onSuccess(it) }
                } catch (t: Throwable) {
                    callback?.onError(t)
                } finally {
                    handleQueuedConfigRequests()
                    isExecuting.set(false)
                }
            }
        }
    }

    /**
     * Builds a new Anonymous User and fetches the latest config
     *
     * [callback] is optional and provided by the SDK user and will callback with the Map of
     * Variables in the latest config when fetched from the API
     */
    @Synchronized
    fun resetUser(callback: DVCCallback<Map<String, Variable<Any>>>? = null) {
        val newUser: User = User.builder().build()
        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(newUser, callback))
        } else {
            flushEvents()

            coroutineScope.launch(coroutineContext) {
                isExecuting.set(true)
                try {
                    fetchConfig(newUser)
                    config?.variables?.let { callback?.onSuccess(it) }
                } catch (t: Throwable) {
                    callback?.onError(t)
                } finally {
                    handleQueuedConfigRequests()
                    isExecuting.set(false)
                }
            }
        }
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
            e.message?.let { Timber.e(it) }
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
        coroutineScope.launch(coroutineContext) {
            try {
                eventQueue.flushEvents(throwOnFailure = true)
                callback?.onSuccess("Successfully flushed events")
            } catch (t: Throwable) {
                callback?.onError(t)
            }
        }
    }

    private fun handleQueuedConfigRequests() {
        coroutineScope.launch(coroutineContext) {
            configRequestMutex.withLock {
                if (configRequestQueue.isEmpty()) {
                    return@withLock
                }

                var latestUserAndCallback: UserAndCallback = configRequestQueue.remove()
                val callbacks: MutableList<DVCCallback<Map<String, Variable<Any>>>> =
                    mutableListOf()

                if (latestUserAndCallback.callback != null) {
                    callbacks.add(latestUserAndCallback.callback!!)
                }
                val itr = configRequestQueue.iterator()

                while (itr.hasNext()) {
                    val userAndCallback = itr.next()
                    if (userAndCallback.now > latestUserAndCallback.now) {
                        latestUserAndCallback = userAndCallback
                    }
                    if (userAndCallback.callback != null) {
                        callbacks.add(userAndCallback.callback)
                    }
                    itr.remove()
                }

                val localUser = latestUserAndCallback.user

                try {
                    fetchConfig(localUser)
                    config?.variables?.let { v ->
                        callbacks.forEach {
                            it.onSuccess(v)
                        }
                    }
                } catch (t: Throwable) {
                    callbacks.forEach {
                        it.onError(t)
                    }
                }
            }
        }
    }

    private fun addUserConfigResultToEventQueue(now: Long, user: User, result: BucketedUserConfig) {
        eventQueue.queueEvent(
            Event.fromInternalEvent(
                Event.userConfigEvent(
                    BigDecimal(System.currentTimeMillis() - now)
                ),
                user,
                result.featureVariationMap
            )
        )
    }

    private fun initializeEventQueue() {
        val flushInMs: Long = options?.flushEventsIntervalMs ?: defaultIntervalInMs
        timer.schedule(eventQueue, flushInMs, flushInMs)
    }

    private fun saveUser() {
        dvcSharedPrefs.save(user, DVCSharedPrefs.UserKey)
    }

    private suspend fun fetchConfig(user: User) {
        val now = System.currentTimeMillis()
        request.getConfigJson(environmentKey, user).collect { config = it }
        observable.configUpdated(config)
        dvcSharedPrefs.save(config, DVCSharedPrefs.ConfigKey)

        this@DVCClient.user = user
        saveUser()
        addUserConfigResultToEventQueue(now, this@DVCClient.user, config!!)
    }

    class DVCClientBuilder {
        private var context: Context? = null
        private var environmentKey: String? = null
        private var user: User? = null
        private var options: DVCOptions? = null
        private var logLevel: LogLevel = LogLevel.ERROR
        private var apiUrl: String = DVCApiClient.BASE_URL

        private var dvcUser: DVCUser? = null

        fun withContext(context: Context): DVCClientBuilder {
            this.context = context
            return this
        }
        fun withEnvironmentKey(environmentKey: String): DVCClientBuilder {
            this.environmentKey = environmentKey
            return this
        }

        fun withUser(user: DVCUser): DVCClientBuilder {
            this.dvcUser = user
            return this
        }

        fun withOptions(options: DVCOptions): DVCClientBuilder {
            this.options = options
            return this
        }

        fun withLogLevel(logLevel: LogLevel): DVCClientBuilder {
            this.logLevel = logLevel
            return this
        }

        @TestOnly
        internal fun withApiUrl(apiUrl: String): DVCClientBuilder {
            this.apiUrl = apiUrl
            return this
        }

        fun build(): DVCClient {
            requireNotNull(context) { "Context must be set" }
            require(!(environmentKey == null || environmentKey == "")) { "SDK key must be set" }
            requireNotNull(dvcUser) { "User must be set" }

            this.user = User.builder().withUserParam(dvcUser!!, context!!).build()

            return DVCClient(context!!, environmentKey!!, user!!, options, logLevel, apiUrl)
        }
    }

    companion object {
        fun builder(): DVCClientBuilder {
            return DVCClientBuilder()
        }
    }

    init {
        saveUser()
        if (logLevel.value > 0) {
            Timber.plant(LogTree(logLevel.value))
        }
    }
}