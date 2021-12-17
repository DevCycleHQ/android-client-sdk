package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.DVCResponse
import com.devcycle.sdk.android.model.UserAndEvents
import retrofit2.Call
import retrofit2.http.*

internal interface DVCEventsApi {
    @Headers("Content-Type:application/json")
    @POST("/v1/events")
    fun trackEvents(
        @Body eventsBody: UserAndEvents
    ): Call<DVCResponse?>
}