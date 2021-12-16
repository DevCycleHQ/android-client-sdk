package com.devcycle.sdk.android.model

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.*

internal class Event constructor(
    private var type: String,
    private var target: String? = null,
    private var date: Long,
    private var value: BigDecimal? = null,
    private var metaData: Any? = null
){
    fun getType(): String {
        return type
    }

    fun getTarget(): String? {
        return target
    }

    fun getDate(): Long {
        return date
    }

    fun getValue(): BigDecimal? {
        return value
    }

    fun getMetaData(): Any? {
        return metaData
    }

    fun setType(type: String) {
        this.type = type
    }

    fun setTarget(target: String) {
        this.target = target
    }

    fun setDate(date: Long) {
        this.date = date
    }

    fun setValue(value: BigDecimal) {
        this.value = value
    }

    fun setMetaData(metaData: Any) {
        this.metaData = metaData
    }

    companion object {
        fun fromDVCEvent(dvcEvent: DVCEvent): Event {
            return Event(
                dvcEvent.type,
                dvcEvent.target,
                dvcEvent.date.time,
                dvcEvent.value,
                dvcEvent.metaData
            )
        }
    }
}