package com.devcycle.sdk.android.model

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.*

class DVCEvent private constructor(
    @Schema(required = true, description = "Custom event type")
    var type: String,

    @Schema(description = "Unix epoch time the event occurred according to client")
    var date: Date = Date(),

    @Schema(description = "Custom event target / subject of event. Contextual to event type")
    var target: String,

    @Schema(description = "Value for numerical events. Contextual to event type")
    var value: BigDecimal,

    @Schema(description = "Extra JSON metadata for event. Contextual to event type")
    var metaData: Map<String, Any>
) {}