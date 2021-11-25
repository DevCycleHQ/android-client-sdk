package com.devcycle.sdk.client.api

import com.devcycle.sdk.client.api.DVCOptions.DVCOptionsBuilder
import com.devcycle.sdk.client.api.DVCOptions

class DVCOptions(
    private val environmentConfigPollingIntervalMs: Int,
    private val flushEventsIntervalMs: Int,
    private val disableEventLogging: Boolean
) {
    class DVCOptionsBuilder internal constructor() {
        private var environmentConfigPollingIntervalMs = 0
        private var flushEventsIntervalMs = 0
        private var disableEventLogging = false
        fun environmentConfigPollingIntervalMs(environmentConfigPollingIntervalMs: Int): DVCOptionsBuilder {
            this.environmentConfigPollingIntervalMs = environmentConfigPollingIntervalMs
            return this
        }

        fun flushEventsIntervalMs(flushEventsIntervalMs: Int): DVCOptionsBuilder {
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
        fun builder(): DVCOptionsBuilder {
            return DVCOptionsBuilder()
        }
    }
}