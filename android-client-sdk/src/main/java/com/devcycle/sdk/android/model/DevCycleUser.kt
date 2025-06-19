package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty

data class DevCycleUser private constructor(
    private var _userId: String?,
    private var _isAnonymous: Boolean?,
    var email: String? = null,
    var name: String? = null,
    var country: String? = null,
    var customData: Map<String, Any>? = null,
    var privateCustomData: Map<String, Any>? = null
) {
    // Public read-only properties
    val userId: String? get() = _userId
    val isAnonymous: Boolean? get() = _isAnonymous
    
    // Internal setters for SDK use only
    @JvmSynthetic
    internal fun setUserId(value: String?) {
        _userId = value
    }
    
    @JvmSynthetic
    internal fun setIsAnonymous(value: Boolean?) {
        _isAnonymous = value
    }

    class Builder internal constructor() {
        private var userId: String? = null
        private var isAnonymous: Boolean? = null
        private var email: String? = null
        private var name: String? = null
        private var country: String? = null
        private var customData: Map<String, Any>? = null
        private var privateCustomData: Map<String, Any>? = null

        @JsonProperty("user_id")
        fun withUserId(userId: String): Builder {
            this.userId = userId
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

        fun build(): DevCycleUser {
            // Note: User validation (userId/isAnonymous logic) is handled in DevCycleClient.build() method
            return DevCycleUser(
                _userId = userId,
                _isAnonymous = isAnonymous,
                email = email,
                name = name,
                country = country,
                customData = customData,
                privateCustomData = privateCustomData
            )
        }

        override fun toString(): String {
            return "User.UserBuilder(userId=$userId, isAnonymous=$isAnonymous, email=$email, name=$name, country=$country, customData=$customData, privateCustomData=$privateCustomData)"
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}

@Deprecated("DVCUser is deprecated, use DevCycleUser instead")
typealias DVCUser = DevCycleUser