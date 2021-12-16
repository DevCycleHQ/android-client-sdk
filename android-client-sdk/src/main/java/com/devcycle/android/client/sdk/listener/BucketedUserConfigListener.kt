package com.devcycle.android.client.sdk.listener

import com.devcycle.sdk.android.model.BucketedUserConfig
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

class BucketedUserConfigListener {
    object BucketedUserConfigObserverConstants {
        const val propertyChangeConfigUpdated = "configUpdated";
    }
    private var config: BucketedUserConfig? = null
    private val support: PropertyChangeSupport = PropertyChangeSupport(this)

    fun addPropertyChangeListener(listener: PropertyChangeListener) {
        support.addPropertyChangeListener(listener)
    }

    fun removePropertyChangeListener(listener: PropertyChangeListener) {
        support.removePropertyChangeListener(listener)
    }

    fun configUpdated(config: BucketedUserConfig?) {
        support.firePropertyChange(BucketedUserConfigObserverConstants.propertyChangeConfigUpdated, this.config, config)
        this.config = config
    }
}