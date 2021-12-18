package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

internal class DVCRequestEvent internal constructor(
    var type: String,
    @field:JsonProperty("target") val target: String?,
    var date: Long? = Calendar.getInstance().time.time,
    var value: BigDecimal? = null,
    var metaData: Map<String, Any>? = null
)