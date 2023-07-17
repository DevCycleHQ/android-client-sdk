package com.devcycle.sdk.android.api

interface DevCycleCallback<T> {
    fun onSuccess(result: T)
    fun onError(t: Throwable)
}

@Deprecated("DVCCallback is deprecated, use DevCycleCallback instead")
typealias DVCCallback<T> = DevCycleCallback<T>