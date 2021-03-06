@file:JvmSynthetic
package com.devcycle.sdk.android.model

import android.content.Context
import com.fasterxml.jackson.annotation.JsonProperty
import android.os.Build
import com.devcycle.sdk.android.BuildConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*
import android.content.pm.PackageInfo
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.IllegalArgumentException

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = User.Builder::class)
internal data class User private constructor(
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
    val appBuild: Long?,
    @Schema(description = "User's custom data to target the user with, data will be logged to DevCycle for use in dashboard.")
    val customData: Map<String, Any>?,
    @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
    val privateCustomData: Map<String, Any>?,
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
    @JvmSynthetic internal fun copyUserAndUpdateFromDVCUser(user: DVCUser): User {
        if (this.userId != user.userId) {
            throw IllegalArgumentException("Cannot update a user with a different userId")
        }

        return this.copy(
            email = user.email,
            name = user.name,
            country = user.country,
            customData = user.customData,
            privateCustomData = user.privateCustomData,
            lastSeenDate = Calendar.getInstance().time.time
        )
    }

    @JsonPOJOBuilder
    internal class Builder internal constructor() {
        @JsonIgnoreProperties(ignoreUnknown = true)

        private var userId: String? = null
        private var isAnonymous: Boolean? = null
        private var email: String? = null
        private var name: String? = null
        private var language: String? = null
        private var country: String? = null
        private var appVersion: String? = null
        private var appBuild: Long? = null
        private var customData: Map<String, Any>? = null
        private var privateCustomData: Map<String, Any>? = null

        private var createdDate = Calendar.getInstance().time.time
        private var platform = "Android"
        private var platformVersion = Build.VERSION.RELEASE
        private var deviceModel = Build.MODEL
        private var sdkType = "mobile"
        private var sdkVersion = BuildConfig.VERSION_NAME
        private var lastSeenDate = Calendar.getInstance().time.time

        @JsonProperty("user_id")
        fun withUserId(userId: String?): Builder {
            this.userId = userId
            return this
        }

        fun withIsAnonymous(isAnonymous: Boolean): Builder {
            this.isAnonymous = isAnonymous
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

        fun withCustomData(customData: Map<String, Any>?): Builder {
            this.customData = customData
            return this
        }

        fun withPrivateCustomData(privateCustomData: Map<String, Any>?): Builder {
            this.privateCustomData = privateCustomData
            return this
        }

        private fun withLanguage(language: String?): Builder {
            this.language = language
            return this
        }

        private fun withAppVersion(appVersion: String?): Builder {
            this.appVersion = appVersion
            return this
        }

        private fun withAppBuild(appBuild: Long?): Builder {
            this.appBuild = appBuild
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

        @JvmSynthetic internal fun withUserParam(user: DVCUser, context: Context): Builder {
            this.userId = user.userId
            this.isAnonymous = user.isAnonymous ?: false
            this.email = user.email
            this.name = user.name
            this.country = user.country
            this.customData = user.customData
            this.privateCustomData = user.privateCustomData

            this.lastSeenDate = Calendar.getInstance().time.time

            val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                context.resources.configuration.locale
            }

            val packageManager = context.packageManager
            val packageInfo: PackageInfo = packageManager.getPackageInfo(context.packageName, 0)

            this.appVersion = packageInfo.versionName
            this.appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                 packageInfo.versionCode.toLong()
            }
            this.language = locale.language

            return this
        }

        @JvmSynthetic internal fun build(): User {
            if (isAnonymous == true) {
                this.userId = UUID.randomUUID().toString()
            }

            if (userId == null && (isAnonymous != true)) {
                throw IllegalArgumentException("Missing userId and isAnonymous is not true")
            } else {

                return User(
                    userId!!,
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
                    isAnonymous!!,
                    lastSeenDate
                )
            }
        }

        override fun toString(): String {
            return "User.UserBuilder(userId=$userId, email=$email, name=$name, language=$language, country=$country, appVersion=$appVersion, appBuild=$appBuild, customData=$customData, privateCustomData=$privateCustomData, createdDate=$createdDate, platform=$platform, platformVersion=$platformVersion, deviceModel=$deviceModel, sdkType=$sdkType, sdkVersion=$sdkVersion)"
        }
    }

    internal companion object {
        @JvmSynthetic internal fun builder(): Builder {
            return Builder()
        }
    }
}