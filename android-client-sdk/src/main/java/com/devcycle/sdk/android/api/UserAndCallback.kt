package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.PopulatedUser
import com.devcycle.sdk.android.model.ReadOnlyVariable
import com.devcycle.sdk.android.model.Variable

internal class UserAndCallback internal constructor(
    val user: PopulatedUser,
    val callback: DVCCallback<Map<String, ReadOnlyVariable<Any>>>?
){
    val now: Long = System.currentTimeMillis()
}