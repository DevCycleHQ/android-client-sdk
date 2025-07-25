@file:JvmSynthetic
package com.devcycle.sdk.android.model

import android.content.Context
import com.fasterxml.jackson.annotation.JsonProperty
import android.os.Build
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*
import android.content.pm.PackageInfo
import com.devcycle.BuildConfig
import com.devcycle.sdk.android.util.DevCycleLogger
import com.devcycle.sdk.android.util.DVCSharedPrefs
import kotlin.IllegalArgumentException

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class PopulatedUser constructor(
    @Schema(required = true, description = "Unique id to identify the user")
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("isAnonymous")
    val isAnonymous: Boolean = false,
    @Schema(description = "User's email used to identify the user on the dashboard / target audiences")
    val email: String? = null,
    @Schema(description = "User's name used to identify the user on the dashboard / target audiences")
    val name: String? = null,
    @Schema(description = "User's language in ISO 639-1 format")
    val language: String? = null,
    @Schema(description = "User's country in ISO 3166 alpha-2 format")
    val country: String? = null,
    @Schema(description = "App Version of the running application")
    val appVersion: String? = null,
    @Schema(description = "App Build number of the running application")
    val appBuild: Long? = null,
    @Schema(description = "User's custom data to target the user with, data will be logged to DevCycle for use in dashboard.")
    val customData: Map<String, Any>? = null,
    @Schema(description = "User's custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing.")
    val privateCustomData: Map<String, Any>? = null,
    @Schema(description = "Date the user was created, Unix epoch timestamp format")
    val createdDate: Long = Calendar.getInstance().time.time,
    @Schema(description = "Platform the Client SDK is running on")
    val platform: String = "Android",
    @Schema(description = "Version of the platform the Client SDK is running on")
    val platformVersion: String = Build.VERSION.RELEASE,
    @Schema(description = "User's device model")
    val deviceModel: String = Build.MODEL,
    @Schema(description = "DevCycle SDK type")
    val sdkType: String = "mobile",
    @Schema(description = "DevCycle SDK Version")
    val sdkVersion: String = BuildConfig.VERSION_NAME,
    @Schema(description = "Date the user was last seen, Unix epoch timestamp format")
    val lastSeenDate: Long? = Calendar.getInstance().time.time,
) {
    @Throws(IllegalArgumentException::class)
    @JvmSynthetic internal fun copyUserAndUpdateFromDevCycleUser(user: DevCycleUser): PopulatedUser {
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

    internal companion object {
        @JvmSynthetic internal fun buildAnonymous(context: Context, dvcSharedPrefs: DVCSharedPrefs): PopulatedUser {
            val userId = dvcSharedPrefs.getOrCreateAnonUserId()
            val isAnonymous = true
            val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                context.resources.configuration.locale
            }
            val packageManager = context.packageManager
            val packageInfo: PackageInfo = packageManager.getPackageInfo(context.packageName, 0)

            val appVersion = packageInfo.versionName
            val appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
            val language = locale.language
            return PopulatedUser(
                userId,
                isAnonymous,
                email = null,
                name = null,
                language,
                country = null,
                appVersion,
                appBuild
            )
        }

        @JvmSynthetic internal fun getLanguage(context: Context): String {
            return try {
                val locale: Locale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locale = context.resources.configuration.locales[0]
                } else {
                    locale = context.resources.configuration.locale
                }
                locale.language
            } catch (e: Exception) {
                DevCycleLogger.e(e, "Error getting language")
                "en"
            }
        }

        @JvmSynthetic internal fun getAppBuildAndVersion(context: Context): Pair<String, Long> {
            return try {
                val packageManager = context.packageManager
                val packageInfo: PackageInfo = packageManager.getPackageInfo(context.packageName, 0)

                val version = packageInfo.versionName ?: "unknown"
                val build = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }
                version to build
            } catch (e: Exception) {
                DevCycleLogger.e(e, "Error getting appVersion or appBuild")
                "unknown" to 0L
            }
        }

        @JvmSynthetic internal fun fromUserParam(user: DevCycleUser, context: Context): PopulatedUser {
            val language = getLanguage(context)

            // User validation should have been done before calling this method
            val userId = requireNotNull(user.userId) { "User must have userId set (should be validated before calling fromUserParam)" }
            val isAnonymous = user.isAnonymous ?: false
            
            val email = user.email
            val name = user.name
            val country = user.country
            val customData = user.customData
            val privateCustomData = user.privateCustomData

            // Get package info safely
            val (appVersion, appBuild) = getAppBuildAndVersion(context)

            return PopulatedUser(
                userId,
                isAnonymous,
                email,
                name,
                language,
                country,
                appVersion,
                appBuild,
                customData,
                privateCustomData,
            )
        }
    }
}