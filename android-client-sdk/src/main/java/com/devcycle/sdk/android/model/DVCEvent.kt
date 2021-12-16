package com.devcycle.sdk.android.model

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.*

class DVCEvent constructor(
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
) {}