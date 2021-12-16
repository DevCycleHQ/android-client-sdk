package com.devcycle.sdk.android.api

import com.devcycle.android.client.sdk.model.DVCResponse
import com.devcycle.android.client.sdk.model.UserAndEvents
import retrofit2.Call
import retrofit2.http.*

internal interface DVCEventsApi {
    @POST("/v1/track")
    fun trackEvents(
        @Query("envKey") envKey: String?,
        @Body eventsBody: UserAndEvents
    ): Call<DVCResponse?>
}