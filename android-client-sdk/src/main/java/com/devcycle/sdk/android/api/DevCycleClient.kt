package com.devcycle.sdk.android.api

import android.app.Application
import android.content.Context
import android.os.Handler
import com.devcycle.sdk.android.eventsource.*
import com.devcycle.sdk.android.exception.DVCRequestException
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.DevCycleLogger
import com.devcycle.sdk.android.util.DVCSharedPrefs
import com.devcycle.sdk.android.util.LogLevel
import com.launchdarkly.eventsource.ConnectStrategy
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.background.BackgroundEventSource
import com.launchdarkly.eventsource.MessageEvent
import com.launchdarkly.eventsource.ReadyState
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

private const val EVENT_SOURCE_RETRY_DELAY_MIN: Long = 5

/**
 * Main entry point for SDK user
 * The class is constructed by calling DevCycleClient.builder(){builder options}.build()
 *
 * All methods that make requests to the APIs or access [config] and [user] are [Synchronized] to
 * ensure thread-safety
 */
class DevCycleClient private constructor(
    private val context: Context,
    private val sdkKey: String,
    private var user: PopulatedUser,
    options: DevCycleOptions?,
    apiUrl: String,
    eventsUrl: String,
    private val customLifecycleHandler: Handler? = null,
    private val coroutineScope: CoroutineScope = MainScope(),
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) {
    private var config: BucketedUserConfig? = null
    private var backgroundEventSource: BackgroundEventSource? = null
    private val defaultIntervalInMs: Long = 10000
    private val flushInMs: Long = options?.flushEventsIntervalMs ?: defaultIntervalInMs
    
    private val configRequestQueue = ConcurrentLinkedQueue<UserAndCallback>()
    private val configRequestMutex = Mutex()
    private val defaultCacheTTL = 30 * 24 * 3600000L // 30 days
    private val configCacheTTL = options?.configCacheTTL ?: defaultCacheTTL
    private val disableConfigCache = options?.disableConfigCache ?: false
    private val disableRealtimeUpdates = options?.disableRealtimeUpdates ?: false
    private val disableAutomaticEventLogging = options?.disableAutomaticEventLogging ?: false
    private val disableCustomEventLogging = options?.disableCustomEventLogging ?: false
    
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context, configCacheTTL)
    private val request: Request = Request(sdkKey, apiUrl, eventsUrl, context)
    private val observable: BucketedUserConfigListener = BucketedUserConfigListener()
    private val enableEdgeDB: Boolean = options?.enableEdgeDB ?: false
    private val isInitialized = AtomicBoolean(false)
    private val isExecuting = AtomicBoolean(false)
    private val isConfigCached = AtomicBoolean(false)
    private val initializeJob: Deferred<Any>

    private val eventQueue: EventQueue = EventQueue(request, ::user, CoroutineScope(coroutineContext), flushInMs)


    private var latestIdentifiedUser: PopulatedUser = user

    private val variableInstanceMap: MutableMap<String, MutableMap<Any, WeakReference<Variable<*>>>> = mutableMapOf()

    init {
        useCachedConfigForUser(user)

        initializeJob = coroutineScope.async(coroutineContext) {
            isExecuting.set(true)
            try {
                fetchConfig(user)
                isInitialized.set(true)
                withContext(Dispatchers.IO){
                    initEventSource()
                    val application : Application = context.applicationContext as Application

                    val lifecycleCallbacks = DVCLifecycleCallbacks(
                        onPauseApplication,
                        onResumeApplication,
                        config?.sse?.inactivityDelay?.toLong(),
                        customLifecycleHandler
                    )
                    application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                }

            } catch (t: Throwable) {
                DevCycleLogger.e(t, "DevCycle SDK Failed to Initialize!")
                throw t
            }
        }

        initializeJob.invokeOnCompletion {
            coroutineScope.launch(coroutineContext) {
                handleQueuedConfigRequests()
                isExecuting.set(false)
            }
        }
    }

    private val onPauseApplication = fun () {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                DevCycleLogger.d("Closing Realtime Updates connection")
                backgroundEventSource?.close()
            }
        }
    }

    private val onResumeApplication = fun () {
        if (backgroundEventSource?.eventSource?.state != ReadyState.OPEN) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    backgroundEventSource?.close()
                    DevCycleLogger.d("Attempting to restart Realtime Updates connection")
                    initEventSource()
                    refetchConfig(false, null, null)
                }
            }
        }
    }

    private fun initEventSource () {
        if (disableRealtimeUpdates) {
            DevCycleLogger.i("Realtime Updates disabled via initialization parameter")
            return
        }
        if (config?.sse?.url == null) { return }

        val handler = SSEEventHandler(fun(messageEvent: MessageEvent?) {
            if (messageEvent == null) {
                return
            }

            val data = JSONObject(messageEvent.data)
            if (!data.has("data")) {
                return
            }

            val innerData = JSONObject(data.get("data") as String)
            val lastModified = if (innerData.has("lastModified")) {
                (innerData.get("lastModified") as Long)
            } else null
            val type = if (innerData.has("type")) {
                (innerData.get("type") as String).toLong()
            } else ""
            val etag = if (innerData.has("etag")) {
                (innerData.get("etag") as String)
            } else null

            if (type == "refetchConfig" || type == "") { // Refetch the config if theres no type
                DevCycleLogger.d("SSE Message: Refetching config")
                refetchConfig(true, lastModified, etag)
            }
        })
        val builder = EventSource.Builder(
            ConnectStrategy.http(URI(config?.sse?.url))
                .readTimeout(EVENT_SOURCE_RETRY_DELAY_MIN, TimeUnit.MINUTES)
        )

        backgroundEventSource = BackgroundEventSource.Builder(handler, builder).build()
        backgroundEventSource?.start()
    }

    fun onInitialized(callback: DevCycleCallback<String>) {
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
    @JvmOverloads
    @Synchronized
    fun identifyUser(user: DevCycleUser, callback: DevCycleCallback<Map<String, BaseConfigVariable>>? = null) {
        // Validate user before modifying any client state
        val updatedUser: PopulatedUser = try {
            if (this@DevCycleClient.user.userId == user.userId) {
                this@DevCycleClient.user.copyUserAndUpdateFromDevCycleUser(user)
            } else {
                // Apply the same validation logic as in DevCycleClientBuilder
                DevCycleClient.validateDevCycleUser(user, dvcSharedPrefs)
                PopulatedUser.fromUserParam(user, context)
            }
        } catch (t: Throwable) {
            // On invalid user, return error and keep existing user/config
            callback?.onError(t)
            return
        }

        flushEvents()

        // Store previous state for recovery
        val previousUser = latestIdentifiedUser
        latestIdentifiedUser = updatedUser

        fetchConfigForUser(updatedUser, object : DevCycleCallback<Map<String, BaseConfigVariable>> {
            override fun onSuccess(variables: Map<String, BaseConfigVariable>) {
                callback?.onSuccess(variables)
            }

            override fun onError(error: Throwable) {
                DevCycleLogger.d("Error fetching config for user_id %s: %s", updatedUser.userId, error.message)

                // In the event that the config request fails (i.e. the device is offline)
                // Fallback to using a Cached Configuration for the User if available
                val hasCachedConfig = tryLoadCachedConfigForUser(updatedUser)
                if (hasCachedConfig) {
                    // Successfully used cached config, return success
                    config?.variables?.let { callback?.onSuccess(it) }
                } else {
                    // No cached config available, restore previous state and return error
                    latestIdentifiedUser = previousUser
                    callback?.onError(error)
                }
            }
        })
    }

    /**
     * Builds a new Anonymous User and fetches the latest config
     *
     * [callback] is optional and provided by the SDK user and will callback with the Map of
     * Variables in the latest config when fetched from the API
     */
    @JvmOverloads
    @Synchronized
    fun resetUser(callback: DevCycleCallback<Map<String, BaseConfigVariable>>? = null) {
        val cachedAnonUserId = dvcSharedPrefs.getAnonUserId()
        if (cachedAnonUserId != null) {
            dvcSharedPrefs.clearAnonUserId()
        }

        // Store previous state for recovery
        val previousUser = latestIdentifiedUser
        val newUser = PopulatedUser.buildAnonymous(this.context, dvcSharedPrefs)
        latestIdentifiedUser = newUser

        flushEvents()

        fetchConfigForUser(newUser, object : DevCycleCallback<Map<String, BaseConfigVariable>> {
            override fun onSuccess(variables: Map<String, BaseConfigVariable>) {
                callback?.onSuccess(variables)
            }

            override fun onError(error: Throwable) {
                // Restore the original anonymous user id and previous user on error
                if (cachedAnonUserId != null) {
                    dvcSharedPrefs.setAnonUserId(cachedAnonUserId)
                }
                latestIdentifiedUser = previousUser
                callback?.onError(error)
            }
        })
    }

    fun close(callback: DevCycleCallback<String>? = null) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                backgroundEventSource?.close()
            }
            withContext(coroutineContext) {
                eventQueue.close(callback)
            }
        }
    }
    /**
     * Returns the Map of Features in the config
     */
    @Synchronized
    fun allFeatures(): Map<String, Feature>? {
        return config?.features ?: emptyMap()
    }

    /**
     * Returns the Map of Variables in the config
     */
    @Synchronized
    fun allVariables(): Map<String, BaseConfigVariable>? {
        return config?.variables ?: emptyMap()
    }

    /**
     * Retrieve a Variable Value. Update the Variable whenever the Config is updated using
     * [java.beans.PropertyChangeListener]
     *
     * [key] is used to identify the Variable in the config
     * [defaultValue] is set on the Variable and used to provide a default value if the Variable
     * could not be fetched or does not exist
     */
    fun variableValue(key: String, defaultValue: String): String {
        return this._variable(key, defaultValue).value
    }

    fun variableValue(key: String, defaultValue: Number): Number {
        return this._variable(key,defaultValue).value
    }

    fun variableValue(key: String, defaultValue: Boolean): Boolean {
        return this._variable(key, defaultValue).value
    }

    fun variableValue(key: String, defaultValue: JSONObject): JSONObject {
        return this._variable(key, defaultValue).value
    }

    fun variableValue(key: String, defaultValue: JSONArray): JSONArray {
        return this._variable(key, defaultValue).value
    }

    /**
     * Retrieve a Variable Object. Update the Variable whenever the Config is updated using
     * [java.beans.PropertyChangeListener]
     *
     * [key] is used to identify the Variable in the config
     * [defaultValue] is set on the Variable and used to provide a default value if the Variable
     * could not be fetched or does not exist
     */
    fun variable(key: String, defaultValue: String): Variable<String> {
        return this._variable(key, defaultValue)
    }

    fun variable(key: String, defaultValue: Number): Variable<Number> {
        return this._variable(key, defaultValue)
    }

    fun variable(key: String, defaultValue: Boolean): Variable<Boolean> {
        return this._variable(key, defaultValue)
    }

    fun variable(key: String, defaultValue: JSONObject): Variable<JSONObject> {
        return this._variable(key, defaultValue)
    }

    fun variable(key: String, defaultValue: JSONArray): Variable<JSONArray> {
        return this._variable(key, defaultValue)
    }

    @Synchronized
    private fun <T: Any> _variable(key: String, defaultValue: T): Variable<T> {
        Variable.getAndValidateType(defaultValue)
        val variable = this.getCachedVariable(key, defaultValue)

        val tmpConfig = config
        if(!disableAutomaticEventLogging){
            val event: Event = Event.fromInternalEvent(
                Event.variableEvent(variable.isDefaulted, variable.key, variable.eval),
                user,
                tmpConfig?.featureVariationMap
            )

            try {
                eventQueue.queueAggregateEvent(event)
            } catch(e: IllegalArgumentException) {
                e.message?.let { DevCycleLogger.e(it) }
            }
        }
        return variable
    }

    private fun <T: Any> getCachedVariable(key: String, defaultValue: T): Variable<T> {
        val variableByKey: BaseConfigVariable? = config?.variables?.get(key)
        val variable: Variable<T>

        if (!variableInstanceMap.containsKey(key)) {
            variableInstanceMap[key] = mutableMapOf()
        }

        val variableFromMap = variableInstanceMap[key]?.get(defaultValue)?.get()

        // if entry does not exist or WeakReference is null
        if (variableFromMap == null) {
            variable = Variable.initializeFromVariable(key, defaultValue, variableByKey)
            observable.addPropertyChangeListener(variable)
            variableInstanceMap[key]?.set(defaultValue, WeakReference(variable))
        } else {
            @Suppress("UNCHECKED_CAST")
            variable = variableFromMap as Variable<T>
        }

        return variable
    }

    /**
     * Track a custom event for the current user. Requires the SDK to have finished initializing.
     *
     * [event] instance of an event object to submit
     */
    fun track(event: DevCycleEvent) {
        if (eventQueue.isClosed.get()) {
            DevCycleLogger.d("DevCycle SDK has been closed, skipping call to track")
            return
        }
        if(!disableCustomEventLogging){
            eventQueue.queueEvent(Event.fromDVCEvent(event, user, config?.featureVariationMap))
        }
    }

    /**
     * Manually send all queued events to the API.
     *
     * [callback] optional callback to be notified on success or failure
     */
    @JvmOverloads
    fun flushEvents(callback: DevCycleCallback<String>? = null) {
        coroutineScope.launch {
            withContext(coroutineContext) {
                val result = eventQueue.flushEvents()

                if (result.success) {
                    callback?.onSuccess("Successfully flushed events")
                } else {
                    result.exception?.let { callback?.onError(it) }
                }
            }
        }
    }

    private suspend fun handleQueuedConfigRequests() {
        configRequestMutex.withLock {
            /*
             * In case a queued request callback adds another item to the queue
             * iterate until the queue is empty
             */
            while (!configRequestQueue.isEmpty()) {
                var latestUserAndCallback: UserAndCallback = configRequestQueue.remove()
                val callbacks: MutableList<DevCycleCallback<Map<String, BaseConfigVariable>>> =
                    mutableListOf()

                latestUserAndCallback.callback?.let { callback ->
                    callbacks.add(callback)
                }
                val itr = configRequestQueue.iterator()

                while (itr.hasNext()) {
                    val userAndCallback = itr.next()
                    if (userAndCallback.now > latestUserAndCallback.now) {
                        latestUserAndCallback = userAndCallback
                    }
                    userAndCallback.callback?.let { callback ->
                        callbacks.add(callback)
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
            return@withLock
        }
    }

    private suspend fun fetchConfig(
        user: PopulatedUser,
        sse: Boolean? = false,
        lastModified: Long? = null,
        etag: String? = null
    ) {
        val result = request.getConfigJson(sdkKey, user, enableEdgeDB, sse, lastModified, etag)
        config = result
        observable.configUpdated(config)
        dvcSharedPrefs.saveConfig(result, user)
        isConfigCached.set(false)
        DevCycleLogger.d("A new config has been fetched for $user")

        this@DevCycleClient.user = user

        if (checkIfEdgeDBEnabled(result, enableEdgeDB)) {
            if (!user.isAnonymous) {
                try {
                    request.saveEntity(user)
                } catch (exception: DVCRequestException) {
                    DevCycleLogger.e("Error saving user entity for $user. Error: $exception")
                }
            }
        }
    }

    private fun fetchConfigForUser(
        user: PopulatedUser, 
        callback: DevCycleCallback<Map<String, BaseConfigVariable>>
    ) {
        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(user, callback))
            return
        }

        isExecuting.set(true)
        coroutineScope.launch {
            withContext(coroutineContext) {
                try {
                    fetchConfig(user)
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


    private fun refetchConfig(
        sse: Boolean = false,
        lastModified: Long? = null,
        etag: String? = null,
        callback: DevCycleCallback<Map<String, BaseConfigVariable>>? = null
    ) {
        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(latestIdentifiedUser, callback))
            DevCycleLogger.d("Queued refetchConfig request")
            return
        }

        isExecuting.set(true)
        coroutineScope.launch {
            withContext(coroutineContext) {
                try {
                    fetchConfig(latestIdentifiedUser, sse, lastModified, etag)
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

    private fun checkIfEdgeDBEnabled(config: BucketedUserConfig, enableEdgeDB: Boolean): Boolean {
        return if (config.project?.settings?.edgeDB?.enabled == true) {
            enableEdgeDB
        } else {
            DevCycleLogger.d("EdgeDB is not enabled for this project. Only using local user data.")
            return false
        }
    }

    private fun useCachedConfigForUser(user: PopulatedUser) {
        val cachedConfig = if (disableConfigCache) null else dvcSharedPrefs.getConfig(user)
        if (cachedConfig != null) {
            config = cachedConfig
            isConfigCached.set(true)
            DevCycleLogger.d("Loaded config from cache for user_id %s", user.userId)
            observable.configUpdated(config)
        }
    }

    private fun tryLoadCachedConfigForUser(user: PopulatedUser): Boolean {
        val cachedConfig = if (disableConfigCache) null else dvcSharedPrefs.getConfig(user)

        if (cachedConfig != null) {
            config = cachedConfig
            isConfigCached.set(true)
            DevCycleLogger.d("Loaded config from cache for user_id %s", user.userId)
            observable.configUpdated(config)
            return true
        } else {
            return false
        }
    }

    class DevCycleClientBuilder {
        private var context: Context? = null
        private var customLifecycleHandler: Handler? = null
        private var sdkKey: String? = null
        private var user: PopulatedUser? = null
        private var options: DevCycleOptions? = null
        private var logLevel: LogLevel = LogLevel.ERROR
        private var logger: DevCycleLogger.Logger = DevCycleLogger.DebugLogger()
        private var apiUrl: String = DVCApiClient.BASE_URL
        private var eventsUrl: String = DVCEventsApiClient.BASE_URL

        private var dvcUser: DevCycleUser? = null

        private var dvcSharedPrefs: DVCSharedPrefs? = null

        fun withContext(context: Context): DevCycleClientBuilder {
            this.context = context
            return this
        }

        internal fun withHandler(handler: Handler): DevCycleClientBuilder {
            this.customLifecycleHandler = handler
            return this
        }

        fun withSDKKey(sdkKey: String): DevCycleClientBuilder {
            this.sdkKey = sdkKey
            return this
        }

        @Deprecated("Use withSDKKey() instead")
        fun withEnvironmentKey(environmentKey: String): DevCycleClientBuilder {
            this.sdkKey = environmentKey
            return this
        }

        fun withUser(user: DevCycleUser): DevCycleClientBuilder {
            this.dvcUser = user
            return this
        }

        fun withOptions(options: DevCycleOptions): DevCycleClientBuilder {
            this.options = options
            options.apiProxyUrl?.let {
                if (it.isNotEmpty()) {
                    // Override the apiUrl with the apiProxyUrl from options
                    apiUrl = it
                }
            }
            options.eventsApiProxyUrl?.let {
                if (it.isNotEmpty()) {
                    // Override the eventsUrl with the eventsApiProxyUrl from options
                    eventsUrl = it
                }
            }
            return this
        }

        fun withLogLevel(logLevel: LogLevel): DevCycleClientBuilder {
            this.logLevel = logLevel
            return this
        }

        fun withLogger(logger: DevCycleLogger.Logger): DevCycleClientBuilder {
            this.logger = logger
            return this
        }

        fun build(): DevCycleClient {
            val context = requireNotNull(context) { "Context must be set" }
            val sdkKey = requireNotNull(sdkKey) { "SDK key must be set" }
            require(sdkKey.isNotEmpty()) { "SDK key must be set" }
            val dvcUser = requireNotNull(dvcUser) { "User must be set" }

            // Choose the most verbose (lowest value) log level between options and builder
            val effectiveLogLevel = listOfNotNull(options?.logLevel, logLevel).minByOrNull { it.value } ?: LogLevel.ERROR
            
            // Set the minimum log level in DevCycleLogger
            DevCycleLogger.setMinLogLevel(effectiveLogLevel)
            
            // Start the logger if logging is enabled
            if (effectiveLogLevel != LogLevel.NO_LOGGING) {
                DevCycleLogger.start(logger)
            }

            val defaultCacheTTL = 30 * 24 * 3600000L // 30 days
            val configCacheTTL = options?.configCacheTTL ?: defaultCacheTTL
            val dvcSharedPrefs = DVCSharedPrefs(context, configCacheTTL)

            // Apply DevCycleUser validation logic for userId and isAnonymous here because we have access to the sharedPrefs
            DevCycleClient.validateDevCycleUser(dvcUser, dvcSharedPrefs)

            val populatedUser = PopulatedUser.fromUserParam(dvcUser, context)

            return DevCycleClient(context, sdkKey, populatedUser, options, apiUrl, eventsUrl, customLifecycleHandler)
        }
    }

    companion object {
        @JvmStatic
        fun builder(): DevCycleClientBuilder {
            return DevCycleClientBuilder()
        }

        /**
         * Validates and finalizes DevCycleUser userId and isAnonymous properties
         * Matches iOS SDK validation logic
         */
        @JvmSynthetic
        internal fun validateDevCycleUser(user: DevCycleUser, sharedPrefs: DVCSharedPrefs) {
            val hasValidUserId = !user.userId.isNullOrEmpty()
            
            require(!(user.isAnonymous == false && !hasValidUserId)) { "User ID is required when isAnonymous is false" }
            
            // Handle the different cases based on isAnonymous and userId (matching iOS SDK logic)
            when {
                !hasValidUserId -> {
                    // No userId provided - generate anonymous ID (covers both explicit anonymous and default case)
                    user.setUserId(sharedPrefs.getOrCreateAnonUserId())
                    user.setIsAnonymous(true)
                }
                else -> {
                    // Valid userId provided, use it with isAnonymous defaulting to false if not set
                    user.setIsAnonymous(false)
                }
            }
        }
    }
}

@Deprecated("DVCClient is deprecated, use DevCycleClient instead")
typealias DVCClient = DevCycleClient
