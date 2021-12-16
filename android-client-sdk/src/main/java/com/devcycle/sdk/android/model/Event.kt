package com.devcycle.sdk.android.model

import java.math.BigDecimal

internal class Event private constructor(
    private var type: String,
    private var user_id: String,
    private var featureVars: Map<String, String>,
    private var target: String? = null,
    private var clientDate: Long,
    private var value: BigDecimal? = null,
    private var metaData: Map<String, Any>? = null
){
    private var date = clientDate

    fun getType(): String {
        return "customType"
    }

    fun getCustomType(): String {
        return type
    }

    fun getUser_id(): String {
        return user_id
    }

    fun getFeatureVars(): Map<String, String> {
        return featureVars
    }

    fun getTarget(): String? {
        return target
    }

    fun getClientDate(): Long {
        return clientDate
    }

    fun getDate(): Long {
        return date
    }

    fun getValue(): BigDecimal? {
        return value
    }

    fun getMetaData(): Map<String, Any>? {
        return metaData
    }

    fun setType(type: String) {
        this.type = type
    }

    fun setTarget(target: String) {
        this.target = target
    }

    fun setClientDate(date: Long) {
        this.clientDate = date
    }

    fun setValue(value: BigDecimal) {
        this.value = value
    }

    fun setMetaData(metaData: Map<String, Any>) {
        this.metaData = metaData
    }

    companion object {
        fun fromDVCEvent(dvcEvent: DVCEvent, user: User, config: BucketedUserConfig): Event {
            return Event(
                dvcEvent.type,
                user.getUserId(),
                config.featureVariationMap ?: emptyMap(),
                dvcEvent.target,
                dvcEvent.date?.time,
                dvcEvent.value,
                dvcEvent.metaData
            )
        }
    }
}