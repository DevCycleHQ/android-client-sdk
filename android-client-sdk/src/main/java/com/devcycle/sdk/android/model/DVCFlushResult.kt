package com.devcycle.sdk.android.model

data class DVCFlushResult(
    val success: Boolean = false,
    val exception: Throwable? = null
)