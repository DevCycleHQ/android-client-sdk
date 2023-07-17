package com.devcycle.example

import com.devcycle.sdk.android.api.DevCycleClient
import java.lang.ref.WeakReference

object DevCycleManager {
    var dvcClient: WeakReference<DevCycleClient> = WeakReference(null)

    fun setClient(client: DevCycleClient) {
        dvcClient = WeakReference(client)
    }
}