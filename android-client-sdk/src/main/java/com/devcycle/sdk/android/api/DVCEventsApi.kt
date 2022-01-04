package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.DVCResponse
import com.devcycle.sdk.android.model.UserAndEvents
import retrofit2.Response
import retrofit2.http.*

internal interface DVCEventsApi {
    @Headers("Content-Type:application/json")
    @POST("/v1/events")
    suspend fun trackEvents(
        @Body eventsBody: UserAndEvents
    ): Response<DVCResponse>
}