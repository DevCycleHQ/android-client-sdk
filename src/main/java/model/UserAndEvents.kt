package model

import lombok.Data
import model.UserAndEvents
import java.util.ArrayList

@Data
class UserAndEvents {
    private var events: MutableList<Event>? = null
    private val user: User? = null
    fun addEventItem(eventItem: Event): UserAndEvents {
        if (events == null) {
            events = ArrayList()
        }
        events!!.add(eventItem)
        return this
    }
}