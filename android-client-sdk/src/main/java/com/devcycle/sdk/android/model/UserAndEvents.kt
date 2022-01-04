package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty

internal data class UserAndEvents(
    @JsonProperty("user")
    val user: User,
    @JsonProperty("events")
    val events: List<Event>?
) {
}