package com.devcycle.sdk.android.model

import java.util.ArrayList

internal class UserAndEvents(private var user: User, private var events: MutableList<Event>?) {
    fun addEventItem(eventItem: Event): UserAndEvents {
        if (events == null) {
            events = ArrayList()
        }
        events!!.add(eventItem)
        return this
    }

    fun getEvents(): List<Event?>? {
        return events
    }

    fun getUser(): User? {
        return user
    }

    fun setEvents(events: MutableList<Event>?) {
        this.events = events
    }

    fun setUser(user: User?) {
        this.user = user!!
    }
}