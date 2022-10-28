package com.devcycle.example

import com.devcycle.sdk.android.api.DVCClient
import java.lang.ref.WeakReference

object DevCycleManager {
    var dvcClient: WeakReference<DVCClient> = WeakReference(null)

    fun setClient(client: DVCClient) {
        dvcClient = WeakReference(client)
    }
}