package com.devcycle.android.client.sdk.listener

import com.devcycle.android.client.sdk.model.BucketedUserConfig
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

class PCLClient {
    private var config: BucketedUserConfig? = null
    private val support: PropertyChangeSupport = PropertyChangeSupport(this)

    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        support.addPropertyChangeListener(listener)
    }

    fun removePropertyChangeListener(listener: PropertyChangeListener) {
        support.removePropertyChangeListener(listener)
    }

    fun configInitialized(config: BucketedUserConfig?) {
        support.firePropertyChange("configInitialized", this.config, config)
        this.config = config
    }
}