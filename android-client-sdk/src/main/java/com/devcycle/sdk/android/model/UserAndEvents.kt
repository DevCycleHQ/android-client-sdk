package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty

internal class UserAndEvents(
    user: User,
    events: MutableList<Event>?
) {
    @JsonProperty("user")
    private val user: User = user

    @JsonProperty("events")
    private val events: MutableList<Event>? = events
}