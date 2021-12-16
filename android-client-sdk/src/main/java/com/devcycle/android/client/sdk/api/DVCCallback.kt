package com.devcycle.android.client.sdk.api

interface DVCCallback<T> {
    fun onSuccess(result: T)
    fun onError(t: Throwable)
}