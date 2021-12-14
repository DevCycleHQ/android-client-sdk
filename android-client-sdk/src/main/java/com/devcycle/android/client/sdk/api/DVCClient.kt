package com.devcycle.android.client.sdk.api

import android.content.Context
import android.util.Log
import com.devcycle.android.client.sdk.model.*
import com.devcycle.android.client.sdk.util.DVCSharedPrefs
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DVCClient private constructor(
    private val context: Context,
    private val environmentKey: String,
    private var user: User,
    options: DVCOptions?,
) {
    private val TAG = DVCClient::class.simpleName
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context)
    private val request: Request = Request()
    private var config: BucketedUserConfig? = null
    private val lock = ReentrantLock()
    private val lockCondition = lock.newCondition()
    private val isInitialized = AtomicBoolean(false)
    private var onInitialized: Future<Boolean>? = null

    private val executor = Executors.newSingleThreadExecutor { r: Runnable? ->
        val t = Thread(r)
        t.name = "DevCycle.DVCClient.InitThread"
        t.isDaemon = true
        t
    }

    fun initialize(callback: DVCCallback<String?>) {
        fetchConfig(object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
                isInitialized.set(true)
                lock.withLock {
                    lockCondition.signalAll()
                }
                callback.onSuccess("Config loaded")
            }

            override fun onError(t: Throwable?) {
                callback.onError(t)
            }
        })
    }

    fun identifyUser(user: UserParam, callback: DVCCallback<Map<String, Variable<Any>>>) {
        if (this.user.getUserId() == user.userId) {
            this.user.updateUser(user)
        } else {
            this.user = User.builder().withUserParam(user).build()
        }
        fetchConfig(object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
                saveUser()
                config?.variables?.let { callback.onSuccess(it) }
            }

            override fun onError(t: Throwable?) {
                callback.onError(t)
            }
        })
    }

    fun resetUser(callback: DVCCallback<Map<String, Variable<Any>>>) {
        val user: User? = dvcSharedPrefs.getCache(DVCSharedPrefs.UserKey)
        if (user == null || !user.getIsAnonymous()) {
            this.user = User.builder()
                .withIsAnonymous(true)
                .build()
        }
        fetchConfig(object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
                saveUser()
                config?.variables?.let { callback.onSuccess(it) }
            }

            override fun onError(t: Throwable?) {
                callback.onError(t)
            }
        })
    }

    fun allFeatures(): Map<String, Feature>? {
        return if (config == null) emptyMap() else config!!.features
    }

    fun allVariables(): Map<String, Variable<Any>>? {
        return if (config == null) emptyMap() else config!!.variables
    }

    fun <T> variable(key: String, defaultValue: T): Variable<T> {
        val variableByKey: Variable<Any>? = config?.variables?.get(key)
        val variable = Variable.initializeFromVariable(key, defaultValue, variableByKey)

        if (variable.isDefaulted == true) {
            callbackVariableWhenInitialized(key, variable);
        }

        return variable
    }

    fun track(): DVCResponse {
        throw NotImplementedError()
    }

    fun flushEvents() {
        throw NotImplementedError()
    }

    private fun internalInitialize(): Future<Boolean>? {
        return executor.submit<Boolean> {
            while (!isInitialized.get()) {
                lock.withLock {
                    try {
                        lockCondition.await()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(TAG, "Thread interrupted", e)
                    }
                }
            }
            isInitialized.get()
        }
    }

    private fun <T> callbackVariableWhenInitialized(key: String, variable: Variable<T>) {
        executor.submit {
            try {
                val isInitialized = onInitialized!!.get()
                if (isInitialized) {
                    val initializedVariable: Variable<Any>? = config?.variables?.get(key)
                    if (initializedVariable != null) {
                        variable.updateVariable(initializedVariable)
                    }
                }
            } catch (e: ExecutionException) {
                Log.e(TAG, "Task aborted", e)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e(TAG, "Thread interrupted", e)
            }
        }
    }

    private fun saveUser() {
        dvcSharedPrefs.save(user, DVCSharedPrefs.UserKey)
    }

    private fun <T> fetchConfig(callback: DVCCallback<T>) {
        request.getConfigJson(environmentKey, user, object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
                config = result
                dvcSharedPrefs.save(config, DVCSharedPrefs.ConfigKey)
                callback.onSuccess(result as T)
            }

            override fun onError(t: Throwable?) {
                callback.onError(t)
            }
        })
    }

    class DVCClientBuilder {
        private var context: Context? = null
        private var environmentKey: String? = null
        private var user: User? = null
        private var options: DVCOptions? = null
        fun withContext(context: Context?): DVCClientBuilder {
            this.context = context
            return this
        }
        fun withEnvironmentKey(environmentKey: String?): DVCClientBuilder {
            this.environmentKey = environmentKey
            return this
        }

        fun withUser(user: User?): DVCClientBuilder {
            this.user = user
            return this
        }

        fun withOptions(options: DVCOptions?): DVCClientBuilder {
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
        onInitialized = internalInitialize()
    }

}