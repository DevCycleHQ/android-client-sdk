package com.devcycle.sdk.android.util

import android.content.Context
import android.content.SharedPreferences
import com.devcycle.R
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.PopulatedUser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.*

// TODO: access disk on background thread
internal class DVCSharedPrefs(context: Context, private val configCacheTTL: Long) {
    private var preferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.cached_data),
        Context.MODE_PRIVATE
    )

    init {
        migrateLegacyConfigs()
        cleanupExpiredConfigs()
    }

    companion object {
        const val AnonUserIdKey = "ANONYMOUS_USER_ID"
        const val IdentifiedConfigKey = "IDENTIFIED_CONFIG"
        const val AnonymousConfigKey = "ANONYMOUS_CONFIG"
        const val ExpiryDateSuffix = "EXPIRY_DATE"
        const val MigrationCompletedKey = "MIGRATION_COMPLETED"
    }

    // MARK: - Anonymous User ID Management
    
    /**
     * Sets the anonymous user ID in shared preferences
     */
    @Synchronized
    fun setAnonUserId(anonUserId: String) {
        preferences.edit().putString(AnonUserIdKey, anonUserId).apply()
    }

    /**
     * Gets the anonymous user ID from shared preferences
     */
    fun getAnonUserId(): String? {
        return preferences.getString(AnonUserIdKey, null)
    }

    /**
     * Clears the anonymous user ID from shared preferences
     */
    @Synchronized
    fun clearAnonUserId() {
        preferences.edit().remove(AnonUserIdKey).apply()
    }

    /**
     * Gets the existing anonymous user ID or creates a new one if none exists
     */
    @Synchronized
    fun getOrCreateAnonUserId(): String {
        val existingId = getAnonUserId()
        if (!existingId.isNullOrEmpty()) {
            return existingId
        }
        
        val newId = UUID.randomUUID().toString()
        setAnonUserId(newId)
        return newId
    }

    // MARK: - Config Management

    private fun generateUserConfigKey(userId: String, isAnonymous: Boolean): String {
        val prefix = if (isAnonymous) AnonymousConfigKey else IdentifiedConfigKey
        return "$prefix.$userId"
    }

    private fun generateUserExpiryDateKey(userId: String, isAnonymous: Boolean): String {
        return "${generateUserConfigKey(userId, isAnonymous)}.$ExpiryDateSuffix"
    }

    @Synchronized
    private fun migrateLegacyConfigs() {
        // Check if migration has already been completed
        if (preferences.getBoolean(MigrationCompletedKey, false)) {
            return
        }
        
        try {
            val legacyKeys = listOf(IdentifiedConfigKey, AnonymousConfigKey)
            val editor = preferences.edit()
            var migrationOccurred = false

            for (legacyKey in legacyKeys) {
                val legacyUserIdKey = "$legacyKey.USER_ID"
                val legacyFetchDateKey = "$legacyKey.FETCH_DATE"
                
                val userId = preferences.getString(legacyUserIdKey, null)
                val fetchDateMs = preferences.getLong(legacyFetchDateKey, 0)
                val configString = preferences.getString(legacyKey, null)

                // Attempt migration if we have complete data
                if (userId != null && configString != null && fetchDateMs > 0) {
                    val isAnonymous = legacyKey == AnonymousConfigKey
                    val userKey = generateUserConfigKey(userId, isAnonymous)
                    val userExpiryDateKey = generateUserExpiryDateKey(userId, isAnonymous)
                    
                    // Only migrate if new format doesn't already exist
                    if (!preferences.contains(userKey)) {
                        editor.putString(userKey, configString)
                        editor.putLong(userExpiryDateKey, Calendar.getInstance().timeInMillis + configCacheTTL)
                        DevCycleLogger.d("Migrated legacy config for user ID $userId from key $legacyKey")
                    }
                }
                
                // Always clean up legacy keys if they exist, regardless of migration success
                var keysRemoved = false
                if (preferences.contains(legacyKey)) {
                    editor.remove(legacyKey)
                    keysRemoved = true
                }
                if (preferences.contains(legacyUserIdKey)) {
                    editor.remove(legacyUserIdKey)
                    keysRemoved = true
                }
                if (preferences.contains(legacyFetchDateKey)) {
                    editor.remove(legacyFetchDateKey)
                    keysRemoved = true
                }
                
                if (keysRemoved) {
                    migrationOccurred = true
                }
            }

            // Mark migration as completed, regardless of whether data was migrated
            editor.putBoolean(MigrationCompletedKey, true)
            
            if (migrationOccurred) {
                editor.apply()
                DevCycleLogger.d("Legacy config migration completed")
            } else {
                editor.apply()
                DevCycleLogger.d("Migration check completed - no legacy data found")
            }
        } catch (e: Exception) {
            DevCycleLogger.e(e, "Error during legacy config migration: ${e.message}")
        }
    }

    @Synchronized
    private fun cleanupExpiredConfigs() {
        try {
            val allPrefs = preferences.all
            val currentTimeMs = Calendar.getInstance().timeInMillis
            val editor = preferences.edit()
            var cleanupOccurred = false

            // Find all config keys (both identified and anonymous)
            val configKeys = allPrefs.keys.filter { key ->
                (key.startsWith("$IdentifiedConfigKey.") || key.startsWith("$AnonymousConfigKey.")) &&
                !key.endsWith(".$ExpiryDateSuffix")
            }

            for (configKey in configKeys) {
                val expiryDateKey = "$configKey.$ExpiryDateSuffix"
                val expiryDateMs = preferences.getLong(expiryDateKey, 0)
                
                // If expiry date exists and is in the past, remove both config and expiry date
                if (expiryDateMs > 0 && expiryDateMs <= currentTimeMs) {
                    editor.remove(configKey)
                    editor.remove(expiryDateKey)
                    cleanupOccurred = true
                    DevCycleLogger.d("Cleaned up expired config: $configKey")
                }
            }

            if (cleanupOccurred) {
                editor.apply()
                DevCycleLogger.d("Expired config cleanup completed")
            }
        } catch (e: Exception) {
            DevCycleLogger.e(e, "Error during expired config cleanup: ${e.message}")
        }
    }



    @Synchronized
    fun remove(key: String?) {
        try {
            val editor: SharedPreferences.Editor = preferences.edit()
            editor.remove(key)
            editor.commit()
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
        }
    }

    fun getString(key: String): String? {
        val stringValue = preferences.getString(key,null)
        if (stringValue == null) {
            DevCycleLogger.i("%s could not be found in SharedPreferences file: %s", key, R.string.cached_data)
            return null
        }
        return stringValue
    }

    @Synchronized
    fun saveString(value: String, key: String) {
        try {
            preferences.edit().putString(key, value).apply()
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
        }
    }

    @Synchronized
    fun saveConfig(configToSave: BucketedUserConfig, user: PopulatedUser) {
        try {
            val userKey = generateUserConfigKey(user.userId, user.isAnonymous)
            val userExpiryDateKey = generateUserExpiryDateKey(user.userId, user.isAnonymous)

            val editor = preferences.edit()
            val jsonString = JSONMapper.mapper.writeValueAsString(configToSave)
            editor.putString(userKey, jsonString)
            editor.putLong(userExpiryDateKey, Calendar.getInstance().timeInMillis + configCacheTTL)
            editor.apply()
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
        }
    }

    @Synchronized
    fun getConfig(user: PopulatedUser): BucketedUserConfig? {
        try {
            val userKey = generateUserConfigKey(user.userId, user.isAnonymous)
            val userConfigString = preferences.getString(userKey, null)
            val userExpiryDateKey = generateUserExpiryDateKey(user.userId, user.isAnonymous)
            val userExpiryDateMs = preferences.getLong(userExpiryDateKey, 0)
            
            val currentTimeMs = Calendar.getInstance().timeInMillis
            
            if (userConfigString != null) {
                if (userExpiryDateMs > currentTimeMs) {
                    DevCycleLogger.d("Loaded config from cache for user ID ${user.userId}")
                    return JSONMapper.mapper.readValue(userConfigString)
                } else {
                    // Config exists but is expired, remove it
                    val editor = preferences.edit()
                    editor.remove(userKey)
                    editor.remove(userExpiryDateKey)
                    editor.apply()
                    DevCycleLogger.d("Removed expired config for user ID ${user.userId}")
                }
            }
            
            DevCycleLogger.d("No valid config found for user ID ${user.userId}")
            return null
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
            return null
        }
    }
}