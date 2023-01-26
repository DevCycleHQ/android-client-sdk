package com.devcycle.sdk.android.model

import com.google.gson.annotations.SerializedName

internal data class UserAndEvents(
    @SerializedName("user")
    val user: PopulatedUser,
    @SerializedName("events")
    val events: List<Event>
)