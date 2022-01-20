package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty

class DVCUser private constructor(
    var userId: String? = null,
    var email: String? = null,
    var name: String? = null,
    var country: String? = null,
    var customData: Any? = null,
    var privateCustomData: Any? = null
) {
    class Builder internal constructor() {
        private var userId: String? = null
        private var email: String? = null
        private var name: String? = null
        private var country: String? = null
        private var customData: Any? = null
        private var privateCustomData: Any? = null

        @JsonProperty("user_id")
        fun withUserId(userId: String?): Builder {
            this.userId = userId
            return this
        }

        fun withEmail(email: String?): Builder {
            this.email = email
            return this
        }

        fun withName(name: String?): Builder {
            this.name = name
            return this
        }

        fun withCountry(country: String?): Builder {
            this.country = country
            return this
        }

        fun withCustomData(customData: Any?): Builder {
            this.customData = customData
            return this
        }

        fun withPrivateCustomData(privateCustomData: Any?): Builder {
            this.privateCustomData = privateCustomData
            return this
        }

        fun build(): DVCUser {
            return DVCUser(
                userId,
                email,
                name,
                country,
                customData,
                privateCustomData
            )
        }

        override fun toString(): String {
            return "User.UserBuilder(userId=$userId, email=$email, name=$name, country=$country, customData=$customData, privateCustomData=$privateCustomData)"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}