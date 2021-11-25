package model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import model.User.UserBuilder

@Data
class User internal constructor(
    @field:JsonProperty("user_id") @field:Schema(description = "Unique id to identify the user. Must be defined if `isAnonymous = false`") private val userId: String?,
    @field:Schema(
        description = "Email used for identifying a device user in the dashboard or used for audience segmentation"
    ) private val email: String?,
    @field:Schema(description = "UName of the user which can be used for identifying a device user, or used for audience segmentation.") private val name: String?,
    @field:Schema(
        description = "SO 639-1 two letter codes, or ISO 639-2 three letter codes"
    ) private val language: String?,
    @field:Schema(description = "ISO 3166 two or three letter codes") private val country: String?,
    @field:Schema(
        description = "Application Version, can be used for audience segmentation."
    ) private val appVersion: String?,
    @field:Schema(description = "Application Build, can be used for audience segmentation.") private val appBuild: String?,
    @field:Schema(
        description = "Custom JSON data used for audience segmentation, must be limited to __kb in size. Values will be logged to DevCycle's servers and available in the dashboard to view."
    ) private val customData: Any?,
    @field:Schema(description = "Private Custom JSON data used for audience segmentation, must be limited to __kb in size. Values will not be logged to DevCycle's servers and will not be available in the dashboard.") private val privateCustomData: Any?,
    @field:Schema(
        description = "Set by SDK automatically."
    ) private val createdDate: Long?,
    @field:Schema(description = "Set by SDK automatically.") private val lastSeenDate: Long?,
    @field:Schema(
        description = "Set by SDK to `android`."
    ) private val platform: String?,
    @field:Schema(description = "Set by SDK to ??") private val platformVersion: String?,
    @field:Schema(
        description = "Set by SDK to user's device model"
    ) private val deviceModel: String?,
    @field:Schema(description = "DevCycle SDK type") private val sdkType: String?,
    @field:Schema(
        description = "DevCycle SDK Version"
    ) private val sdkVersion: String?
) {
    @Schema(
        required = true,
        description = "Users must be explicitly defined as anonymous, where the SDK will generate a random `user_id` for them. If they are `isAnonymous = false` a `user_id` value must be provided."
    )
    private val isAnonymous = false

    class UserBuilder internal constructor() {
        private var userId: String? = null
        private var email: String? = null
        private var name: String? = null
        private var language: String? = null
        private var country: String? = null
        private var appVersion: String? = null
        private var appBuild: String? = null
        private var customData: Any? = null
        private var privateCustomData: Any? = null
        private var createdDate: Long? = null
        private var lastSeenDate: Long? = null
        private var platform: String? = null
        private var platformVersion: String? = null
        private var deviceModel: String? = null
        private var sdkType: String? = null
        private var sdkVersion: String? = null
        @JsonProperty("user_id")
        fun userId(userId: String?): UserBuilder {
            this.userId = userId
            return this
        }

        fun email(email: String?): UserBuilder {
            this.email = email
            return this
        }

        fun name(name: String?): UserBuilder {
            this.name = name
            return this
        }

        fun language(language: String?): UserBuilder {
            this.language = language
            return this
        }

        fun country(country: String?): UserBuilder {
            this.country = country
            return this
        }

        fun appVersion(appVersion: String?): UserBuilder {
            this.appVersion = appVersion
            return this
        }

        fun appBuild(appBuild: String?): UserBuilder {
            this.appBuild = appBuild
            return this
        }

        fun customData(customData: Any?): UserBuilder {
            this.customData = customData
            return this
        }

        fun privateCustomData(privateCustomData: Any?): UserBuilder {
            this.privateCustomData = privateCustomData
            return this
        }

        fun createdDate(createdDate: Long?): UserBuilder {
            this.createdDate = createdDate
            return this
        }

        fun lastSeenDate(lastSeenDate: Long?): UserBuilder {
            this.lastSeenDate = lastSeenDate
            return this
        }

        fun platform(platform: String?): UserBuilder {
            this.platform = platform
            return this
        }

        fun platformVersion(platformVersion: String?): UserBuilder {
            this.platformVersion = platformVersion
            return this
        }

        fun deviceModel(deviceModel: String?): UserBuilder {
            this.deviceModel = deviceModel
            return this
        }

        fun sdkType(sdkType: String?): UserBuilder {
            this.sdkType = sdkType
            return this
        }

        fun sdkVersion(sdkVersion: String?): UserBuilder {
            this.sdkVersion = sdkVersion
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
                lastSeenDate,
                platform,
                platformVersion,
                deviceModel,
                sdkType,
                sdkVersion
            )
        }

        override fun toString(): String {
            return "User.UserBuilder(userId=$userId, email=$email, name=$name, language=$language, country=$country, appVersion=$appVersion, appBuild=$appBuild, customData=$customData, privateCustomData=$privateCustomData, createdDate=$createdDate, lastSeenDate=$lastSeenDate, platform=$platform, platformVersion=$platformVersion, deviceModel=$deviceModel, sdkType=$sdkType, sdkVersion=$sdkVersion)"
        }
    }

    companion object {
        fun builder(): UserBuilder {
            return UserBuilder()
        }
    }
}