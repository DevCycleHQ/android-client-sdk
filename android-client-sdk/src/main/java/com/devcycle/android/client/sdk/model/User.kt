package com.devcycle.android.client.sdk.model

import com.fasterxml.jackson.annotation.JsonProperty
import android.os.Build
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import java.util.*

@Data
class User private constructor(
    userId: String?,
    email: String?,
    name: String?,
    language: String?,
    country: String?,
    appVersion: String?,
    appBuild: String?,
    customData: Any?,
    privateCustomData: Any?,
    createdDate: Long,
    platform: String,
    platformVersion: String,
    deviceModel: String,
    sdkType: String,
    sdkVersion: String,
    isAnonymous: Boolean
) {
    @JsonProperty("isAnonymous")
    private val isAnonymous: Boolean

    @Schema(required = true, description = "Unique id to identify the user")
    @JsonProperty("user_id")
    private val userId: String

    @Schema(description = "User's email used to identify the user on the dashboard / target audiences")
    @JsonProperty("email")
    private val email: String?

    @Schema(description = "User's name used to identify the user on the dashboard / target audiences")
    @JsonProperty("name")
    private val name: String?

    @Schema(description = "User's language in ISO 639-1 format")
    @JsonProperty("language")
    private val language: String?

    @Schema(description = "User's country in ISO 3166 alpha-2 format")
    @JsonProperty("country")
    private val country: String?

    @Schema(description = "App Version of the running application")
    @JsonProperty("appVersion")
    private val appVersion: String?

    @Schema(description = "App Build number of the running application")
    @JsonProperty("appBuild")
    private val appBuild: String?

    @Schema(description = "User's custom data to target the user with, data will be logged to DevCycle for use in dashboard.")
    @JsonProperty("customData")
    private val customData: Any?

    @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
    @JsonProperty("privateCustomData")
    private val privateCustomData: Any?

    @Schema(description = "Date the user was created, Unix epoch timestamp format")
    @JsonProperty("createdDate")
    private val createdDate: Long

    @Schema(description = "Date the user was last seen, Unix epoch timestamp format")
    @JsonProperty("lastSeenDate")
    private var lastSeenDate: Long

    @Schema(description = "Platform the Client SDK is running on")
    @JsonProperty("platform")
    private val platform: String

    @Schema(description = "Version of the platform the Client SDK is running on")
    @JsonProperty("platformVersion")
    private val platformVersion: String

    @Schema(description = "User's device model")
    @JsonProperty("deviceModel")
    private val deviceModel: String

    @Schema(description = "DevCycle SDK type")
    @JsonProperty("sdkType")
    private val sdkType: String

    @Schema(description = "DevCycle SDK Version")
    @JsonProperty("sdkVersion")
    private val sdkVersion: String

    fun updateUser() {
        lastSeenDate = Calendar.getInstance().time.time
    }

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
        private val createdDate = Calendar.getInstance().time.time
        private val platform = "Android"
        private val platformVersion = Build.VERSION.RELEASE
        private val deviceModel = Build.MODEL
        private val sdkType = "client"
        private val sdkVersion = "0.0.1"

        fun userId(userId: String?): Builder {
            this.userId = userId
            return this
        }

        fun email(email: String?): Builder {
            this.email = email
            return this
        }

        fun name(name: String?): Builder {
            this.name = name
            return this
        }

        fun language(language: String?): Builder {
            this.language = language
            return this
        }

        fun country(country: String?): Builder {
            this.country = country
            return this
        }

        fun appVersion(appVersion: String?): Builder {
            this.appVersion = appVersion
            return this
        }

        fun appBuild(appBuild: String?): Builder {
            this.appBuild = appBuild
            return this
        }

        fun customData(customData: Any?): Builder {
            this.customData = customData
            return this
        }

        fun privateCustomData(privateCustomData: Any?): Builder {
            this.privateCustomData = privateCustomData
            return this
        }

        fun isAnonymous(isAnonymous: Boolean): Builder {
            this.isAnonymous = isAnonymous
            return this
        }

        fun build(): User {
            return User(
                userId,
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
                isAnonymous
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

    init {
        this.userId = if (isAnonymous && userId == "") UUID.randomUUID().toString() else userId!!
        this.isAnonymous = isAnonymous
        this.email = email
        this.name = name
        this.language = language
        this.country = country
        this.appVersion = appVersion
        this.appBuild = appBuild
        this.customData = customData
        this.privateCustomData = privateCustomData
        this.createdDate = createdDate
        this.platform = platform
        this.platformVersion = platformVersion
        this.deviceModel = deviceModel
        this.sdkType = sdkType
        this.sdkVersion = sdkVersion
        lastSeenDate = Calendar.getInstance().time.time
    }
}