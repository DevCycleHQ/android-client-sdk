package com.devcycle.android.client.sdk.api

import android.content.Context
import com.devcycle.android.client.sdk.model.*
import com.devcycle.android.client.sdk.util.DVCSharedPrefs

class DVCClient private constructor(
    private val context: Context,
    private val environmentKey: String,
    private var user: User,
    options: DVCOptions?,
) {
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context)
    private val request: Request = Request()
    private var config: BucketedUserConfig? = null
    private fun saveUser() {
        dvcSharedPrefs.save(user, DVCSharedPrefs.UserKey)
    }

    fun initialize(callback: DVCCallback<String?>) {
        fetchConfig(object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
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

    fun <T> variable(key: String?, defaultValue: T): Variable<T> {
        throw NotImplementedError()
    }

    fun track(): DVCResponse {
        throw NotImplementedError()
    }

    fun flushEvents() {
        throw NotImplementedError()
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
    }

}