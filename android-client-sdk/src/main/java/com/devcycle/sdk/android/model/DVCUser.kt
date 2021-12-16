package com.devcycle.sdk.android.model

import java.util.*

class DVCUser constructor(
    var isAnonymous: Boolean,
    userId: String? = null,
    var email: String? = null,
    var name: String? = null,
    var language: String? = null,
    var country: String? = null,
    var appVersion: String? = null,
    var appBuild: String? = null,
    var customData: Any? = null,
    var privateCustomData: Any? = null
) {
    var userId: String = if (isAnonymous && userId == "") UUID.randomUUID().toString() else userId!!
}