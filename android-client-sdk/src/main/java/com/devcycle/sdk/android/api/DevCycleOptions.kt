package com.devcycle.sdk.android.api

class DevCycleOptions(
    val flushEventsIntervalMs: Long,
    val disableEventLogging: Boolean,
    val enableEdgeDB: Boolean,
    val configCacheTTL: Long?,
    val disableConfigCache: Boolean,
    val disableRealtimeUpdates: Boolean,
    val disableAutomaticEventLogging : Boolean,
    val disableCustomEventLogging : Boolean
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
        fun build(): DevCycleOptions {
            return DevCycleOptions(
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
        fun builder(): DevCycleOptionsBuilder {
            return DevCycleOptionsBuilder()
        }
    }
}

@Deprecated("DVCOptions is deprecated, use DevCycleOptions instead")
typealias DVCOptions = DevCycleOptions