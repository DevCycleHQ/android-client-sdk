package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.util.LogLevel

class DevCycleOptions(
    val flushEventsIntervalMs: Long,
    val disableEventLogging: Boolean,
    val enableEdgeDB: Boolean,
    val configCacheTTL: Long?,
    val disableConfigCache: Boolean,
    val disableRealtimeUpdates: Boolean,
    val disableAutomaticEventLogging : Boolean,
    val disableCustomEventLogging : Boolean,
    val apiProxyUrl: String?,
    val eventsApiProxyUrl: String?,
    val logLevel: LogLevel?
) {
    class DevCycleOptionsBuilder internal constructor() {
        private var flushEventsIntervalMs = 0L
        private var disableEventLogging = false
        private var enableEdgeDB = false
        private var configCacheTTL: Long? = null
        private var disableConfigCache = false
        private var disableRealtimeUpdates = false
        private var disableAutomaticEventLogging = false
        private var disableCustomEventLogging = false
        private var apiProxyUrl: String? = null
        private var eventsApiProxyUrl: String? = null
        private var logLevel: LogLevel? = null

        fun flushEventsIntervalMs(flushEventsIntervalMs: Long): DevCycleOptionsBuilder {
            this.flushEventsIntervalMs = flushEventsIntervalMs
            return this
        }

        @Deprecated("Use disableAutomaticEventLogging or disableCustomEventLogging")
        fun disableEventLogging(disableEventLogging: Boolean): DevCycleOptionsBuilder {
            this.disableEventLogging = disableEventLogging
            return this
        }

        fun disableAutomaticEventLogging(disableAutomaticEventLogging: Boolean): DevCycleOptionsBuilder{
            this.disableAutomaticEventLogging = disableAutomaticEventLogging
            return this
        }

        fun disableCustomEventLogging(disableCustomEventLogging: Boolean): DevCycleOptionsBuilder{
            this.disableCustomEventLogging = disableCustomEventLogging
            return this
        }

        fun enableEdgeDB(enableEdgeDB: Boolean): DevCycleOptionsBuilder {
            this.enableEdgeDB = enableEdgeDB
            return this
        }

        fun configCacheTTL(configCacheTTL: Long): DevCycleOptionsBuilder {
            this.configCacheTTL = configCacheTTL
            return this
        }

        fun disableConfigCache(disableConfigCache: Boolean): DevCycleOptionsBuilder {
            this.disableConfigCache = disableConfigCache
            return this
        }
        
        fun disableRealtimeUpdates(disableRealtimeUpdates: Boolean): DevCycleOptionsBuilder {
            this.disableRealtimeUpdates = disableRealtimeUpdates
            return this
        }

        fun apiProxyUrl(apiProxyUrl: String): DevCycleOptionsBuilder {
            this.apiProxyUrl = apiProxyUrl
            return this
        }

        fun eventsApiProxyUrl(eventsApiProxyUrl: String): DevCycleOptionsBuilder {
            this.eventsApiProxyUrl = eventsApiProxyUrl
            return this
        }
        
        fun withLogLevel(logLevel: LogLevel): DevCycleOptionsBuilder {
            this.logLevel = logLevel
            return this
        }
        
        fun build(): DevCycleOptions {
            return DevCycleOptions(
                flushEventsIntervalMs,
                disableEventLogging,
                enableEdgeDB,
                configCacheTTL,
                disableConfigCache,
                disableRealtimeUpdates,
                disableAutomaticEventLogging,
                disableCustomEventLogging,
                apiProxyUrl,
                eventsApiProxyUrl,
                logLevel
            )
        }
    }

    companion object {
        @JvmStatic
        fun builder(): DevCycleOptionsBuilder {
            return DevCycleOptionsBuilder()
        }
    }
}

@Deprecated("DVCOptions is deprecated, use DevCycleOptions instead")
typealias DVCOptions = DevCycleOptions