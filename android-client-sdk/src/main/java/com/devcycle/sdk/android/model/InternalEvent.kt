package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

/**
 * Represents an event logged internally by the SDK, before it has been enriched with user and
 * config data. "Type" is treated as the actual event type, rather than the "customType"
 */
internal class InternalEvent internal constructor(
    var type: String,
    @field:JsonProperty("target") val target: String? = null,
    var date: Long? = Calendar.getInstance().time.time,
    var value: BigDecimal? = null,
    var metaData: Map<String, Any>? = null
)