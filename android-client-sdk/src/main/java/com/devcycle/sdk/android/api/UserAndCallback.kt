package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable

class UserAndCallback internal constructor(
    val user: DVCUser,
    val callback: DVCCallback<Map<String, Variable<Any>>>?,
    val userAction: UserAction
){
    val now: Long = System.currentTimeMillis()
}

enum class UserAction {
    IDENTIFY_USER,
    RESET_USER
}