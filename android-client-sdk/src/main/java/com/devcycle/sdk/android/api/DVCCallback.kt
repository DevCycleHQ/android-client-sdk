package com.devcycle.sdk.android.api

interface DVCCallback<T> {
    fun onSuccess(result: T)
    fun onError(t: Throwable)
}