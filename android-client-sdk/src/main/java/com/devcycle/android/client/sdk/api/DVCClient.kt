package com.devcycle.android.client.sdk.api

import android.content.Context
import android.util.Log
import com.devcycle.android.client.sdk.model.*
import com.devcycle.android.client.sdk.util.DVCSharedPrefs

class DVCClient private constructor(
    private val context: Context,
    private val environmentKey: String,
    private val user: User,
    options: DVCOptions?,
) {
    private val dvcSharedPrefs: DVCSharedPrefs = DVCSharedPrefs(context)
    private val request: Request = Request()
    private var config: BucketedUserConfig? = null
    private fun saveUser() {
        dvcSharedPrefs.save(user, DVCSharedPrefs.UserKey)
    }

    fun initialize(callback: DVCCallback<String?>) {
        request.getConfigJson(environmentKey, user, object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
                config = result
                dvcSharedPrefs.save(config, DVCSharedPrefs.ConfigKey)
                callback.onSuccess("Config loaded")
            }

            override fun onError(t: Throwable?) {
                callback.onError(t)
            }
        })
    }

    fun identifyUser(): Variable {
        throw NotImplementedError()
    }

    fun resetUser() {
        throw NotImplementedError()
    }

    fun allFeatures(): Map<String, Feature> {
        throw NotImplementedError()
    }

    fun allVariables(): Map<String, Feature> {
        throw NotImplementedError()
    }

    fun <T> variable(key: String?, defaultValue: T): Variable {
        throw NotImplementedError()
    }

    fun track(): DVCResponse {
        throw NotImplementedError()
    }

    fun flushEvents() {
        throw NotImplementedError()
    }

    fun fetchConfig() {
        throw NotImplementedError()
    }

    class DVCClientBuilder {
        private var context: Context? = null;
        private var environmentKey: String? = null
        private var user: User? = null
        private var options: DVCOptions? = null
        fun withContext(context: Context?): DVCClientBuilder {
            this.context = context;
            return this;
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