package com.devcycle.android.client.sdk.api

import com.devcycle.android.client.sdk.model.*

class DVCClient private constructor(
    private val environmentKey: String,
    private val user: User,
    options: DVCOptions?
) {
    private val request: Request = Request()
    private var config: BucketedUserConfig? = null
    private fun saveUser() {
        throw NotImplementedError()
    }

    fun initialize(callback: DVCCallback<String?>) {
        request.getConfigJson(environmentKey, user, object : DVCCallback<BucketedUserConfig?> {
            override fun onSuccess(result: BucketedUserConfig?) {
                config = result
                callback.onSuccess("Config loaded")
            }

            override fun onError(t: Throwable?) {}
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
        private var environmentKey: String? = null
        private var user: User? = null
        private var options: DVCOptions? = null
        fun environmentKey(environmentKey: String?): DVCClientBuilder {
            this.environmentKey = environmentKey
            return this
        }

        fun user(user: User?): DVCClientBuilder {
            this.user = user
            return this
        }

        fun options(options: DVCOptions?): DVCClientBuilder {
            this.options = options
            return this
        }

        fun build(): DVCClient {
            require(!(environmentKey == null || environmentKey == "")) { "SDK key must be set" }
            requireNotNull(user) { "User must be set" }
            return DVCClient(environmentKey!!, user!!, options)
        }
    }

    companion object {
        fun builder(): DVCClientBuilder {
            return DVCClientBuilder()
        }
    }

}