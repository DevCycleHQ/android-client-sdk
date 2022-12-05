package com.devcycle.sdk.android.api

class DVCOptions(
    private val environmentConfigPollingIntervalMs: Int,
    val flushEventsIntervalMs: Long,
    private val disableEventLogging: Boolean,
    val enableEdgeDB: Boolean,
    val configCacheTTL: Long,
    val disableConfigCache: Boolean
) {
    class DVCOptionsBuilder internal constructor() {
        private var environmentConfigPollingIntervalMs = 0
        private var flushEventsIntervalMs = 0L
        private var disableEventLogging = false
        private var enableEdgeDB = false
        private var configCacheTTL= 0L
        private var disableConfigCache = false
        fun environmentConfigPollingIntervalMs(environmentConfigPollingIntervalMs: Int): DVCOptionsBuilder {
            this.environmentConfigPollingIntervalMs = environmentConfigPollingIntervalMs
            return this
        }

        fun flushEventsIntervalMs(flushEventsIntervalMs: Long): DVCOptionsBuilder {
            this.flushEventsIntervalMs = flushEventsIntervalMs
            return this
        }

        fun disableEventLogging(disableEventLogging: Boolean): DVCOptionsBuilder {
            this.disableEventLogging = disableEventLogging
            return this
        }

        fun enableEdgeDB(enableEdgeDB: Boolean): DVCOptionsBuilder {
            this.enableEdgeDB = enableEdgeDB
            return this
        }

        fun configCacheTTL(configCacheTTL: Long): DVCOptionsBuilder {
            this.configCacheTTL = configCacheTTL
            return this
        }

        fun disableConfigCache(disableConfigCache: Boolean): DVCOptionsBuilder {
            this.disableConfigCache = disableConfigCache
            return this
        }

        fun build(): DVCOptions {
            return DVCOptions(
                environmentConfigPollingIntervalMs,
                flushEventsIntervalMs,
                disableEventLogging,
                enableEdgeDB,
                configCacheTTL,
                disableConfigCache
            )
        }
    }

    companion object {
        @JvmStatic
        fun builder(): DVCOptionsBuilder {
            return DVCOptionsBuilder()
        }
    }
}