package com.devcycle.sdk.android.model

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.*

class DVCEvent private constructor(
    @Schema(required = true, description = "Custom event type")
    var type: String,

    @Schema(description = "Custom event target / subject of event. Contextual to event type")
    var target: String? = null,

    @Schema(description = "Value for numerical events. Contextual to event type")
    var value: BigDecimal? = null,

    @Schema(description = "Extra JSON metadata for event. Contextual to event type")
    var metaData: Map<String, Any>? = null,

    @Schema(description = "Unix epoch time the event occurred according to client")
    var date: Date = Date(),
) {
    class Builder internal constructor() {
        private lateinit var type: String
        private var target: String? = null
        private var value: BigDecimal? = null
        private var metaData: Map<String, Any>? = null

        fun withType(type: String): Builder {
            this.type = type
            return this
        }

        fun withTarget(target: String?): Builder {
            this.target = target
            return this
        }

        fun withValue(value: BigDecimal?): Builder {
            this.value = value
            return this
        }

        fun withMetaData(metaData: Map<String, Any>?): Builder {
            this.metaData = metaData
            return this
        }

        fun build(): DVCEvent {
            return DVCEvent(
                type,
                target,
                value,
                metaData
            )
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}