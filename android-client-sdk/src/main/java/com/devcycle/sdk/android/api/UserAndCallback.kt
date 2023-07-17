package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.BaseConfigVariable
import com.devcycle.sdk.android.model.PopulatedUser

internal class UserAndCallback internal constructor(
    val user: PopulatedUser,
    val callback: DevCycleCallback<Map<String, BaseConfigVariable>>?
){
    val now: Long = System.currentTimeMillis()
}