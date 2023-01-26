package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*

internal data class Event private constructor(
    @SerializedName("type")
    val type: String,
    @SerializedName("customType")
    val customType: String?,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("featureVars")
    val featureVars: Map<String, String>,
    @SerializedName("target")
    val target: String?,
    @SerializedName("clientDate")
    val clientDate: Long,
    @SerializedName("value")
    val value: BigDecimal? = null,
    @SerializedName("metaData")
    val metaData: Map<String, Any>? = null
){
    @get:JsonProperty("date")
    val date get() = clientDate

    companion object {
        @JvmSynthetic internal fun fromDVCEvent(dvcEvent: DVCEvent, user: PopulatedUser, featureVars: Map<String, String>?): Event {
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
            const val customEvent: String = "customEvent"
        }

        @JvmSynthetic internal fun variableEvent(defaulted: Boolean?, key: String?): InternalEvent {
            val type = if (defaulted == true) EventTypes.variableDefaulted else EventTypes.variableEvaluated

            return InternalEvent(
                type,
                key,
                value = BigDecimal.ONE
            )
        }

        @JvmSynthetic internal fun fromInternalEvent(event: InternalEvent, user: PopulatedUser, featureVars: Map<String, String>?) : Event {
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