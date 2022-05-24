package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class Event private constructor(
    val type: String,
    val customType: String?,
    @JsonProperty("user_id")
    val userId: String,
    val featureVars: Map<String, String>,
    val target: String?,
    val clientDate: Long,
    var value: BigDecimal? = null,
    val metaData: Map<String, Any>? = null
){
    @get:JsonProperty("date")
    val date get() = clientDate

    companion object {
        @JvmSynthetic internal fun fromDVCEvent(dvcEvent: DVCEvent, user: User, featureVars: Map<String, String>?): Event {
            return Event(
                EventTypes.customEvent,
                dvcEvent.type,
                user.userId,
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
                user.userId,
                featureVars ?: emptyMap(),
                event.target,
                event.date ?: Calendar.getInstance().time.time,
                event.value,
                event.metaData
            )
        }
    }
}