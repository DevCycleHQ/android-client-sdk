package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty

class DVCUser private constructor(
    var isAnonymous: Boolean,
    var userId: String? = null,
    var email: String? = null,
    var name: String? = null,
    var language: String? = null,
    var country: String? = null,
    var appVersion: String? = null,
    var appBuild: String? = null,
    var customData: Any? = null,
    var privateCustomData: Any? = null
) {
    class Builder internal constructor() {
        private var userId: String? = null
        private var email: String? = null
        private var name: String? = null
        private var language: String? = null
        private var country: String? = null
        private var appVersion: String? = null
        private var appBuild: String? = null
        private var customData: Any? = null
        private var privateCustomData: Any? = null
        private var isAnonymous = true

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

        fun withLanguage(language: String?): Builder {
            this.language = language
            return this
        }

        fun withCountry(country: String?): Builder {
            this.country = country
            return this
        }

        fun withAppVersion(appVersion: String?): Builder {
            this.appVersion = appVersion
            return this
        }

        fun withAppBuild(appBuild: String?): Builder {
            this.appBuild = appBuild
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

        fun withIsAnonymous(isAnonymous: Boolean): Builder {
            this.isAnonymous = isAnonymous
            return this
        }

        fun build(): DVCUser {
            return DVCUser(
                isAnonymous,
                userId,
                email,
                name,
                language,
                country,
                appVersion,
                appBuild,
                customData,
                privateCustomData
            )
        }

        override fun toString(): String {
            return "User.UserBuilder(userId=$userId, email=$email, name=$name, language=$language, country=$country, appVersion=$appVersion, appBuild=$appBuild, customData=$customData, privateCustomData=$privateCustomData)"
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}