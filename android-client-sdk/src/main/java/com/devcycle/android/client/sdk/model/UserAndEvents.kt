package com.devcycle.android.client.sdk.model

import lombok.Data
import java.util.ArrayList

@Data
class UserAndEvents(private val user: User, private var events: MutableList<Event>?) {
    fun addEventItem(eventItem: Event): UserAndEvents {
        if (events == null) {
            events = ArrayList()
        }
        events!!.add(eventItem)
        return this
    }
}