package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
internal class Event internal constructor(
    type: String,
    customType: String?,
    userId: String,
    featureVars: Map<String, String>,
    @field:JsonProperty("target") val target: String?,
    clientDate: Long,
    value: BigDecimal? = null,
    metaData: Map<String, Any>? = null
){
    @JsonProperty("type")
    var type: String = type

    @JsonProperty("user_id")
    val userId: String = userId

    @JsonProperty("customType")
    val customType: String? = customType

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
        @JvmSynthetic internal fun fromDVCEvent(dvcEvent: DVCEvent, user: User, featureVars: Map<String, String>?): Event {
            return Event(
                EventTypes.customEvent,
                dvcEvent.type,
                user.getUserId(),
                featureVars ?: emptyMap(),
                dvcEvent.target,
                dvcEvent.date.time,
                dvcEvent.value,
                dvcEvent.metaData
            )
        }

        internal object EventTypes {
            const val variableEvaluated: String = "variableEvaluated"
            const val variableDefaulted: String = "variableDefaulted"
            const val userConfig: String = "userConfig"
            const val customEvent: String = "customEvent"
        }

        @JvmSynthetic internal fun userConfigEvent(value: BigDecimal): InternalEvent {
            return InternalEvent(
                type = EventTypes.userConfig,
                value = value
            )
        }

        @JvmSynthetic internal fun variableEvent(defaulted: Boolean?, key: String?): InternalEvent {
            val type = if (defaulted == true) EventTypes.variableDefaulted else EventTypes.variableEvaluated

            return InternalEvent(
                type,
                key
            )
        }

        @JvmSynthetic internal fun fromInternalEvent(event: InternalEvent, user: User, featureVars: Map<String, String>?) : Event {
            return Event(
                event.type,
                null,
                user.getUserId(),
                featureVars ?: emptyMap(),
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