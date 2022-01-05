package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import android.os.Build
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import io.swagger.v3.oas.annotations.media.Schema
import java.lang.IllegalArgumentException
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = User.Builder::class)
internal data class User(
    @Schema(required = true, description = "Unique id to identify the user")
    @JsonProperty("user_id")
    val userId: String,
    @Schema(description = "User's email used to identify the user on the dashboard / target audiences")
    val email: String?,
    @Schema(description = "User's name used to identify the user on the dashboard / target audiences")
    val name: String?,
    @Schema(description = "User's language in ISO 639-1 format")
    val language: String?,
    @Schema(description = "User's country in ISO 3166 alpha-2 format")
    val country: String?,
    @Schema(description = "App Version of the running application")
    val appVersion: String?,
    @Schema(description = "App Build number of the running application")
    val appBuild: String?,
    @Schema(description = "User's custom data to target the user with, data will be logged to DevCycle for use in dashboard.")
    val customData: Any?,
    @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
    val privateCustomData: Any?,
    @Schema(description = "Date the user was created, Unix epoch timestamp format")
    val createdDate: Long,
    @Schema(description = "Platform the Client SDK is running on")
    val platform: String,
    @Schema(description = "Version of the platform the Client SDK is running on")
    val platformVersion: String,
    @Schema(description = "User's device model")
    val deviceModel: String,
    @Schema(description = "DevCycle SDK type")
    val sdkType: String,
    @Schema(description = "DevCycle SDK Version")
    val sdkVersion: String,
    @JsonProperty("isAnonymous")
    val isAnonymous: Boolean,
    @Schema(description = "Date the user was last seen, Unix epoch timestamp format")
    val lastSeenDate: Long?
) {

    @Throws(IllegalArgumentException::class)
    internal fun updateUser(user: DVCUser): User {
        if (this.userId != user.userId) {
            throw IllegalArgumentException("Cannot update a user with a different userId")
        }

        return this.copy(
            email = user.email,
            name = user.name,
            language = user.language,
            country = user.country,
            appVersion = user.appVersion,
            appBuild = user.appBuild,
            customData = user.customData,
            privateCustomData = user.privateCustomData,
            lastSeenDate = Calendar.getInstance().time.time
        )
    }

    @JsonPOJOBuilder
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
        private var createdDate = Calendar.getInstance().time.time
        private var platform = "Android"
        private var platformVersion = Build.VERSION.RELEASE
        private var deviceModel = Build.MODEL
        private var sdkType = "client"
        private var sdkVersion = "0.0.1"
        private var lastSeenDate: Long? = null

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

        private fun withCreatedDate(createdDate: Long): Builder {
            this.createdDate = createdDate
            return this
        }

        private fun withPlatform(platform: String): Builder {
            this.platform = platform
            return this
        }

        private fun withPlatformVersion(platformVersion: String): Builder {
            this.platformVersion = platformVersion
            return this
        }

        private fun withDeviceModel(deviceModel: String): Builder {
            this.deviceModel = deviceModel
            return this
        }

        private fun withSdkType(sdkType: String): Builder {
            this.sdkType = sdkType
            return this
        }

        private fun withSdkVersion(sdkVersion: String): Builder {
            this.sdkVersion = sdkVersion
            return this
        }

        private fun withLastSeenDate(lastSeenDate: Long): Builder {
            this.lastSeenDate = lastSeenDate
            return this
        }

        internal fun withUserParam(user: DVCUser): Builder {
            this.userId = user.userId
            this.email = user.email
            this.name = user.name
            this.language = user.language
            this.country = user.country
            this.appVersion = user.appVersion
            this.appBuild = user.appBuild
            this.customData = user.customData
            this.privateCustomData = user.privateCustomData
            return this
        }

        fun build(): User {
            val isAnonymous = userId == null
            return User(
                if (isAnonymous) UUID.randomUUID().toString() else userId!!,
                email,
                name,
                language,
                country,
                appVersion,
                appBuild,
                customData,
                privateCustomData,
                createdDate,
                platform,
                platformVersion,
                deviceModel,
                sdkType,
                sdkVersion,
                isAnonymous,
                lastSeenDate ?: Calendar.getInstance().time.time
            )
        }

        override fun toString(): String {
            return "User.UserBuilder(userId=$userId, email=$email, name=$name, language=$language, country=$country, appVersion=$appVersion, appBuild=$appBuild, customData=$customData, privateCustomData=$privateCustomData, createdDate=$createdDate, platform=$platform, platformVersion=$platformVersion, deviceModel=$deviceModel, sdkType=$sdkType, sdkVersion=$sdkVersion)"
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}