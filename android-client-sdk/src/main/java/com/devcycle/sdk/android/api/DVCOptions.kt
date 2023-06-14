package com.devcycle.sdk.android.api

class DVCOptions(
    val flushEventsIntervalMs: Long,
    val disableEventLogging: Boolean,
    val enableEdgeDB: Boolean,
    val configCacheTTL: Long?,
    val disableConfigCache: Boolean,
    val disableRealtimeUpdates: Boolean,
    val disableAutomaticEventLogging : Boolean,
    val disableCustomEventLogging : Boolean
) {
    class DVCOptionsBuilder internal constructor() {
        private var flushEventsIntervalMs = 0L
        private var disableEventLogging = false
        private var enableEdgeDB = false
        private var configCacheTTL: Long? = null
        private var disableConfigCache = false
        private var disableRealtimeUpdates = false
        private var disableAutomaticEventLogging = false
        private var disableCustomEventLogging = false

        fun flushEventsIntervalMs(flushEventsIntervalMs: Long): DVCOptionsBuilder {
            this.flushEventsIntervalMs = flushEventsIntervalMs
            return this
        }

        @Deprecated("Use disableAutomaticEventLogging or disableCustomEventLogging")
        fun disableEventLogging(disableEventLogging: Boolean): DVCOptionsBuilder {
            this.disableEventLogging = disableEventLogging
            return this
        }

        fun disableAutomaticEventLogging(disableAutomaticEventLogging: Boolean): DVCOptionsBuilder{
            this.disableAutomaticEventLogging = disableAutomaticEventLogging
            return this
        }

        fun disableCustomEventLogging(disableCustomEventLogging: Boolean): DVCOptionsBuilder{
            this.disableCustomEventLogging = disableCustomEventLogging
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
        fun disableRealtimeUpdates(disableRealtimeUpdates: Boolean): DVCOptionsBuilder {
            this.disableRealtimeUpdates = disableRealtimeUpdates
            return this
        }
        fun build(): DVCOptions {
            return DVCOptions(
                flushEventsIntervalMs,
                disableEventLogging,
                enableEdgeDB,
                configCacheTTL,
                disableConfigCache,
                disableRealtimeUpdates,
                disableAutomaticEventLogging,
                disableCustomEventLogging
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