package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
internal class Event private constructor(
    type: String,
    userId: String,
    featureVars: Map<String, String>,
    target: String?,
    clientDate: Long,
    value: BigDecimal?,
    metaData: Map<String, Any>?
){
    @JsonProperty("type")
    private var type: String = "customType"

    @JsonProperty("user_id")
    private val userId: String = userId

    @JsonProperty("customType")
    private var customType: String = type

    @JsonProperty("featureVars")
    private var featureVars: Map<String, String>? = null
    @JsonProperty("target")
    private var target: String? = null
    @JsonProperty("clientDate")
    private var clientDate: Long? = null
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("value")
    private var value: BigDecimal? = null
    @JsonProperty("metaData")
    private var metaData: Map<String, Any>? = null
    @JsonProperty("date")
    private var date = clientDate

    companion object {
        fun fromDVCEvent(dvcEvent: DVCEvent, user: User, config: BucketedUserConfig): Event {
            return Event(
                dvcEvent.type,
                user.getUserId(),
                config.featureVariationMap ?: emptyMap(),
                dvcEvent.target,
                dvcEvent.date.time,
                dvcEvent.value,
                dvcEvent.metaData
            )
        }
    }

    init {
        this.featureVars = featureVars
        this.target = target
        this.clientDate = clientDate
        this.value = value
        this.metaData = metaData
    }
}