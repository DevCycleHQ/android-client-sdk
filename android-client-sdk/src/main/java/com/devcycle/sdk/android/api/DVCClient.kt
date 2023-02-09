package com.devcycle.sdk.android.api

import android.app.Application
import android.content.Context
import android.os.Handler
import com.devcycle.sdk.android.eventsource.*
import com.devcycle.sdk.android.exception.DVCRequestException
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.model.*
import com.devcycle.sdk.android.util.DVCSharedPrefs
import com.devcycle.sdk.android.util.LogLevel
import com.devcycle.sdk.android.util.LogTree
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.TestOnly
import org.json.JSONObject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.net.URI
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
    private var user: PopulatedUser,
    options: DVCOptions?,
    apiUrl: String,
    eventsUrl: String,
    private val customLifecycleHandler: Handler? = null,
    private val coroutineScope: CoroutineScope = MainScope(),
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) {
    private var config: BucketedUserConfig? = null
    private var eventSource: EventSource? = null
    private val defaultIntervalInMs: Long = 10000
    private val flushInMs: Long = options?.flushEventsIntervalMs ?: defaultIntervalInMs
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context)
    private val request: Request = Request(environmentKey, apiUrl, eventsUrl)
    private val observable: BucketedUserConfigListener = BucketedUserConfigListener()
    private val eventQueue: EventQueue = EventQueue(request, ::user, CoroutineScope(coroutineContext), flushInMs)
    private val enableEdgeDB: Boolean = options?.enableEdgeDB ?: false
    private val isInitialized = AtomicBoolean(false)
    private val isExecuting = AtomicBoolean(false)
    private val isConfigCached = AtomicBoolean(false)
    private val initializeJob: Deferred<Any>

    private val configRequestQueue = ConcurrentLinkedQueue<UserAndCallback>()
    private val configRequestMutex = Mutex()
    private val defaultCacheTTL = 7 * 24 * 3600000L // 7 days
    private val configCacheTTL = options?.configCacheTTL ?: defaultCacheTTL
    private val disableConfigCache = options?.disableConfigCache ?: false

    private var latestIdentifiedUser: PopulatedUser = user

    private val variableInstanceMap: MutableMap<String, MutableMap<Any, WeakReference<Variable<*>>>> = mutableMapOf()

    init {
        val cachedConfig = if (disableConfigCache) null else dvcSharedPrefs.getConfig(user, configCacheTTL)
        if (cachedConfig != null) {
            config = cachedConfig
            isConfigCached.set(true)
            Timber.d("Loaded config from cache")
            observable.configUpdated(config)
        }

        initializeJob = coroutineScope.async(coroutineContext) {
            isExecuting.set(true)
            try {
                fetchConfig(user)
                isInitialized.set(true)
                withContext(Dispatchers.IO){
                    initEventSource()
                    val application : Application = context.applicationContext as Application

                    val lifecycleCallbacks = DVCLifecycleCallbacks(onPauseApplication, onResumeApplication,
                        config?.sse?.inactivityDelay?.toLong(), customLifecycleHandler
                    )
                    application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                }

            } catch (t: Throwable) {
                Timber.e(t, "DevCycle SDK Failed to Initialize!")
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
        Timber.d("Closing streaming event source connection")
        eventSource?.close()
    }

    private val onResumeApplication = fun () {
        if (eventSource?.state != ReadyState.OPEN) {
            eventSource?.close()
            Timber.d("Restarting streaming event source connection")
            initEventSource()
            refetchConfig(false, null)
        }
    }

    private fun initEventSource () {
        if (config?.sse?.url == null) { return }
        eventSource = EventSource.Builder(Handler(fun(messageEvent: MessageEvent?) {
            if (messageEvent == null) { return }

            val data = JSONObject(messageEvent.data)
            if (!data.has("data")) { return }

            val innerData = JSONObject(data.get("data") as String)
            val lastModified = if (innerData.has("lastModified")) {
                (innerData.get("lastModified") as Long)
            } else null

            val type = if (innerData.has("type")) {
                (innerData.get("type") as String).toLong()
            } else ""

            if (type == "refetchConfig" || type == "") { // Refetch the config if theres no type
                refetchConfig(true, lastModified)
            }
        }), URI(config?.sse?.url)).build()
        eventSource?.start()
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
    @JvmOverloads
    @Synchronized
    fun identifyUser(user: DVCUser, callback: DVCCallback<Map<String, ReadOnlyVariable<Any>>>? = null) {
        flushEvents()

        val updatedUser: PopulatedUser = if (this@DVCClient.user.userId == user.userId) {
            this@DVCClient.user.copyUserAndUpdateFromDVCUser(user)
        } else {
            val anonId: String? = dvcSharedPrefs.getString(DVCSharedPrefs.AnonUserIdKey);
            PopulatedUser.fromUserParam(user, context, anonId)
        }
        latestIdentifiedUser = updatedUser

        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(updatedUser, callback))
            Timber.d("Queued identifyUser request for user_id %s", updatedUser.userId)
            return
        }

        isExecuting.set(true)
        coroutineScope.launch {
            withContext(coroutineContext) {
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
    @JvmOverloads
    @Synchronized
    fun resetUser(callback: DVCCallback<Map<String, ReadOnlyVariable<Any>>>? = null) {
        val newUser: PopulatedUser = PopulatedUser.buildAnonymous()
        latestIdentifiedUser = newUser

        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(newUser, callback))
            Timber.d("Queued resetUser request for new anonymous user")
            return
        }

        flushEvents()

        coroutineScope.launch {
            withContext(coroutineContext) {
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

    fun close(callback: DVCCallback<String>? = null) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                eventSource?.close()
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
        return if (config == null) emptyMap() else config!!.features
    }

    /**
     * Returns the Map of Variables in the config
     */
    @Synchronized
    fun allVariables(): Map<String, ReadOnlyVariable<Any>>? {
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
        Variable.getAndValidateType(defaultValue)
        val variable = this.getCachedVariable(key, defaultValue)

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

    private fun <T: Any> getCachedVariable(key: String, defaultValue: T): Variable<T> {
        val variableByKey: ReadOnlyVariable<Any>? = config?.variables?.get(key)
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
    fun track(event: DVCEvent) {
        if (eventQueue.isClosed.get()) {
            Timber.d("DVC sdk has been closed, skipping call to track")
            return
        }
        eventQueue.queueEvent(Event.fromDVCEvent(event, user, config?.featureVariationMap))
    }

    /**
     * Manually send all queued events to the API.
     *
     * [callback] optional callback to be notified on success or failure
     */
    @JvmOverloads
    fun flushEvents(callback: DVCCallback<String>? = null) {
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
                val callbacks: MutableList<DVCCallback<Map<String, ReadOnlyVariable<Any>>>> =
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
            return@withLock
        }
    }

    private fun saveUser() {
        dvcSharedPrefs.save(user, DVCSharedPrefs.UserKey)
        if (user.isAnonymous)
            dvcSharedPrefs.saveString(user.userId, DVCSharedPrefs.AnonUserIdKey)
    }

    private suspend fun fetchConfig(user: PopulatedUser, sse: Boolean? = false, lastModified: Long? = null) {
        val result = request.getConfigJson(environmentKey, user, enableEdgeDB, sse, lastModified)
        config = result
        observable.configUpdated(config)
        dvcSharedPrefs.saveConfig(config!!, user)
        isConfigCached.set(false)
        Timber.d("A new config has been fetched for $user")

        this@DVCClient.user = user
        saveUser()

        if (checkIfEdgeDBEnabled(config!!, enableEdgeDB)) {
            if (!user.isAnonymous) {
                try {
                    request.saveEntity(user)
                } catch (exception: DVCRequestException) {
                    Timber.e("Error saving user entity for $user. Error: $exception")
                }
            }
        }
    }

    private fun refetchConfig(sse: Boolean = false, lastModified: Long? = null, callback: DVCCallback<Map<String, ReadOnlyVariable<Any>>>? = null) {
        if (isExecuting.get()) {
            configRequestQueue.add(UserAndCallback(latestIdentifiedUser, callback))
            Timber.d("Queued refetchConfig request")
            return
        }

        isExecuting.set(true)
        coroutineScope.launch {
            withContext(coroutineContext) {
                try {
                    fetchConfig(latestIdentifiedUser, sse, lastModified)
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
            Timber.d("EdgeDB is not enabled for this project. Only using local user data.")
            return false
        }
    }

    class DVCClientBuilder {
        private var context: Context? = null
        private var customLifecycleHandler: Handler? = null
        private var environmentKey: String? = null
        private var user: PopulatedUser? = null
        private var options: DVCOptions? = null
        private var logLevel: LogLevel = LogLevel.ERROR
        private var tree: Timber.Tree = LogTree(logLevel.value)
        private var apiUrl: String = DVCApiClient.BASE_URL
        private var eventsUrl: String = DVCEventsApiClient.BASE_URL

        private var dvcUser: DVCUser? = null

        private var dvcSharedPrefs: DVCSharedPrefs? = null

        fun withContext(context: Context): DVCClientBuilder {
            this.context = context
            return this
        }

        internal fun withHandler(handler: Handler): DVCClientBuilder {
            this.customLifecycleHandler = handler
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

        fun withLogger(logger: Timber.Tree): DVCClientBuilder {
            this.tree = logger
            return this
        }

        @TestOnly
        @JvmSynthetic internal fun withApiUrl(apiUrl: String): DVCClientBuilder {
            this.apiUrl = apiUrl
            this.eventsUrl = apiUrl
            return this
        }

        fun build(): DVCClient {
            requireNotNull(context) { "Context must be set" }
            require(!(environmentKey == null || environmentKey == "")) { "SDK key must be set" }
            requireNotNull(dvcUser) { "User must be set" }

            if (logLevel.value > 0) {
                Timber.plant(tree)
            }

            dvcSharedPrefs = DVCSharedPrefs(context!!);

            val anonId: String? = dvcSharedPrefs!!.getString(DVCSharedPrefs.AnonUserIdKey)

            this.user = PopulatedUser.fromUserParam(dvcUser!!, context!!, anonId)

            return DVCClient(context!!, environmentKey!!, user!!, options, apiUrl, eventsUrl, customLifecycleHandler)
        }
    }

    companion object {
        @JvmStatic
        fun builder(): DVCClientBuilder {
            return DVCClientBuilder()
        }
    }

    init {
        saveUser()
    }
}