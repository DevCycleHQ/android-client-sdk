package com.devcycle.sdk.android.model

import java.util.*

class DVCUser private constructor(
    var isAnonymous: Boolean,
    userId: String?,
    var email: String?,
    var name: String?,
    var language: String?,
    var country: String?,
    var appVersion: String?,
    var appBuild: String?,
    var customData: Any?,
    var privateCustomData: Any?
) {
    var userId: String = if (isAnonymous && userId == "") UUID.randomUUID().toString() else userId!!
}