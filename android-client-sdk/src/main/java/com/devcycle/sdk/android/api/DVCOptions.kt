package com.devcycle.sdk.android.api

class DVCOptions(
    private val environmentConfigPollingIntervalMs: Int,
    val flushEventsIntervalMs: Long,
    private val disableEventLogging: Boolean
) {
    class DVCOptionsBuilder internal constructor() {
        private var environmentConfigPollingIntervalMs = 0
        private var flushEventsIntervalMs = 0L
        private var disableEventLogging = false
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

        fun build(): DVCOptions {
            return DVCOptions(
                environmentConfigPollingIntervalMs,
                flushEventsIntervalMs,
                disableEventLogging
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