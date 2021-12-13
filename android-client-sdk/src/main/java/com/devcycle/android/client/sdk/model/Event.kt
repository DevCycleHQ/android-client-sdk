package com.devcycle.android.client.sdk.model

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

class Event {
    @Schema(required = true, description = "Custom event type")
    private var type: String? = null

    @Schema(description = "Custom event target / subject of event. Contextual to event type")
    private var target: String? = null

    @Schema(description = "Unix epoch time the event occurred according to client")
    private var date: Long? = null

    @Schema(description = "Value for numerical events. Contextual to event type")
    private var value: BigDecimal? = null

    @Schema(description = "Extra JSON metadata for event. Contextual to event type")
    private var metaData: Any? = null

    fun getType(): String? {
        return type
    }

    fun getTarget(): String? {
        return target
    }

    fun getDate(): Long? {
        return date
    }

    fun getValue(): BigDecimal? {
        return value
    }

    fun getMetaData(): Any? {
        return metaData
    }

    fun setType(type: String?) {
        this.type = type
    }

    fun setTarget(target: String?) {
        this.target = target
    }

    fun setDate(date: Long?) {
        this.date = date
    }

    fun setValue(value: BigDecimal?) {
        this.value = value
    }

    fun setMetaData(metaData: Any?) {
        this.metaData = metaData
    }
}