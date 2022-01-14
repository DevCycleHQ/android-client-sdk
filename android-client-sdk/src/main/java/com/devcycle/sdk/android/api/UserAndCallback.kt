package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.User
import com.devcycle.sdk.android.model.Variable

class UserAndCallback internal constructor(
    val user: User,
    val callback: DVCCallback<Map<String, Variable<Any>>>?
){
    val now: Long = System.currentTimeMillis()
}