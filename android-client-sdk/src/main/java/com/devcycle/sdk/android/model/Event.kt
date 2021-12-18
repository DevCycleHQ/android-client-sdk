package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
internal class Event internal constructor(
    type: String,
    userId: String,
    featureVars: Map<String, String>,
    @field:JsonProperty("target") val target: String?,
    clientDate: Long,
    value: BigDecimal? = null,
    metaData: Map<String, Any>? = null
){
    @JsonProperty("type")
    var type: String = "customType"

    @JsonProperty("user_id")
    val userId: String = userId

    @JsonProperty("customType")
    val customType: String = type

    @JsonProperty("featureVars")
    var featureVars: Map<String, String>? = null

    @JsonProperty("clientDate")
    private var clientDate: Long? = null
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("value")
    var value: BigDecimal? = null
    @JsonProperty("metaData")
    var metaData: Map<String, Any>? = null
    @JsonProperty("date")
    var date = clientDate

    companion object {
        @JvmSynthetic internal fun fromDVCEvent(dvcEvent: DVCEvent, user: User, config: BucketedUserConfig): Event {
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

        internal object EventTypes {
            const val variableEvaluated: String = "variableEvaluated"
            const val variableDefaulted: String = "variableDefaulted"
        }

        @JvmSynthetic internal fun aggregateEvent(defaulted: Boolean?, key: String?): DVCRequestEvent {
            val type = if (defaulted == true) EventTypes.variableDefaulted else EventTypes.variableEvaluated

            return DVCRequestEvent(
                type,
                key
            )
        }

        @JvmSynthetic internal fun fromAggregateEvent(event: DVCRequestEvent, user: User, config: BucketedUserConfig?) : Event {
            return Event(
                event.type,
                user.getUserId(),
                config?.featureVariationMap ?: emptyMap(),
                event.target,
                event.date ?: Calendar.getInstance().time.time,
                event.value,
                event.metaData
            )
        }
    }

    init {
        this.featureVars = featureVars
        this.clientDate = clientDate
        this.value = value
        this.metaData = metaData
    }
}