package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.DVCResponse
import com.devcycle.sdk.android.model.User
import retrofit2.Response
import retrofit2.http.*

internal interface DVCEdgeDBApi {
    @Headers("Content-Type:application/json")
    @PATCH("/v1/edgeDB/{id}")
    suspend fun saveEntity(
        @Path("id") id: String?,
        @Body userBody: User
    ): Response<DVCResponse>
}