package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReadOnlyVariable<T>(
    @JsonProperty("_id")
    val id: String,
    val value: T,
    val key: String,
    val type: Variable.TypeEnum,
    val evalReason: String?
)
