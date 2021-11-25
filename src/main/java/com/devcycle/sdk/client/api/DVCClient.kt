package com.devcycle.sdk.client.api

import com.devcycle.sdk.client.api.DVCOptions
import kotlin.NotImplementedError
import model.DVCResponse
import com.devcycle.sdk.client.api.DVCClient.DVCClientBuilder
import com.devcycle.sdk.client.api.DVCClient
import model.Feature
import model.User
import model.Variable

class DVCClient(environmentKey: String?, user: User?, options: DVCOptions?) {
    fun onClientInitialized(): Boolean {
        throw NotImplementedError()
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

    fun allVariables(): Map<String, Variable> {
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

    class DVCClientBuilder internal constructor() {
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
            return DVCClient(environmentKey, user, options)
        }
    }

    companion object {
        fun builder(): DVCClientBuilder {
            return DVCClientBuilder()
        }
    }
}