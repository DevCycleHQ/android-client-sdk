package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

class DVCUser private constructor(
    @JsonProperty("user_id")
    var userId: String? = null,
    var isAnonymous: Boolean? = true,
    var email: String? = null,
    var name: String? = null,
    var country: String? = null,
    var customData: Map<String, Any>? = null,
    var privateCustomData: Map<String, Any>? = null
) {
    class Builder internal constructor() {
        @JsonIgnoreProperties(ignoreUnknown = true)

        private var userId: String? = null
        private var isAnonymous: Boolean? = true
        private var email: String? = null
        private var name: String? = null
        private var country: String? = null
        private var customData: Map<String, Any>? = null
        private var privateCustomData: Map<String, Any>? = null

        @JsonProperty("user_id")
        fun withUserId(userId: String): Builder {
            this.userId = userId
            this.isAnonymous = false
            return this
        }

        fun withIsAnonymous(isAnonymous: Boolean): Builder {
            this.isAnonymous = isAnonymous
            return this
        }

        fun withEmail(email: String): Builder {
            this.email = email
            return this
        }

        fun withName(name: String): Builder {
            this.name = name
            return this
        }

        fun withCountry(country: String): Builder {
            this.country = country
            return this
        }

        fun withCustomData(customData: Map<String, Any>): Builder {
            this.customData = customData
            return this
        }

        fun withPrivateCustomData(privateCustomData: Map<String, Any>): Builder {
            this.privateCustomData = privateCustomData
            return this
        }

        fun build(): DVCUser {
            return DVCUser(
                userId,
                isAnonymous,
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