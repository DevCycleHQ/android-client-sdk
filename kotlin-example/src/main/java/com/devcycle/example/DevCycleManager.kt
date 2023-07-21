package com.devcycle.example

import com.devcycle.sdk.android.api.DevCycleClient
import java.lang.ref.WeakReference

object DevCycleManager {
    var devCycleClient: WeakReference<DevCycleClient> = WeakReference(null)

    fun setClient(client: DevCycleClient) {
        devCycleClient = WeakReference(client)
    }
}