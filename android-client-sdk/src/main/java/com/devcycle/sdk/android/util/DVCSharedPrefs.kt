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
internal class DVCSharedPrefs(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.cached_data),
        Context.MODE_PRIVATE
    )

    companion object {
        const val UserKey = "USER"
        const val AnonUserIdKey = "ANONYMOUS_USER_ID"
        const val IdentifiedConfigKey = "IDENTIFIED_CONFIG"
        const val AnonymousConfigKey = "ANONYMOUS_CONFIG"
        const val ConfigKeyPrefix = "USER_CONFIG_"
        const val FetchDateSuffix = "_FETCH_DATE"
    }

    @Synchronized
    fun <T> save(objectToSave: T, key: String?) {
        try {
            val jsonString = JSONMapper.mapper.writeValueAsString(objectToSave)
            preferences.edit().putString(key, jsonString).apply()
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
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
            // Generate the user-specific key
            val userKey = ConfigKeyPrefix + user.userId
            val editor = preferences.edit()
            val jsonString = JSONMapper.mapper.writeValueAsString(configToSave)
            editor.putString(userKey, jsonString)
            editor.putLong(userKey + FetchDateSuffix, Calendar.getInstance().timeInMillis)
            
            // For backward compatibility, also save in the old format
            val legacyKey = if (user.isAnonymous) AnonymousConfigKey else IdentifiedConfigKey
            editor.putString(legacyKey, jsonString)
            editor.putString("$legacyKey.USER_ID", user.userId)
            editor.putLong("$legacyKey.FETCH_DATE", Calendar.getInstance().timeInMillis)
            
            editor.apply()
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
        }
    }

    @Synchronized
    fun getConfig(user: PopulatedUser, ttlMs: Long): BucketedUserConfig? {
        try {
            // Try to fetch config using the user-specific key first
            val userKey = ConfigKeyPrefix + user.userId
            val userConfigString = preferences.getString(userKey, null)
            val userFetchDateMs = preferences.getLong(userKey + FetchDateSuffix, 0)
            
            val oldestValidDateMs = Calendar.getInstance().timeInMillis - ttlMs
            
            // If we found a valid user-specific config, use it
            if (userConfigString != null && userFetchDateMs >= oldestValidDateMs) {
                DevCycleLogger.d("Loaded config from cache for user ID ${user.userId}")
                return JSONMapper.mapper.readValue(userConfigString)
            }
            
            // Fall back to the legacy approach for backward compatibility
            val legacyKey = if (user.isAnonymous) AnonymousConfigKey else IdentifiedConfigKey
            val userId = preferences.getString("$legacyKey.USER_ID", null)
            val fetchDateMs = preferences.getLong("$legacyKey.FETCH_DATE", 0)

            if (userId != user.userId) {
                DevCycleLogger.d("Skipping cached config: no config for user ID ${user.userId}")
                return null
            }

            if (fetchDateMs < oldestValidDateMs) {
                DevCycleLogger.d("Skipping cached config: last fetched date is too old")
                return null
            }

            val configString = preferences.getString(legacyKey, null)
            if (configString == null) {
                DevCycleLogger.d("Skipping cached config: no config found")
                return null
            }
            
            DevCycleLogger.d("Loaded legacy config from cache for user ID ${user.userId}")
            return JSONMapper.mapper.readValue(configString)
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
            return null
        }
    }

    /**
     * Cleans up old configs that exceed the TTL
     * This is an internal method that can be called periodically
     */
    @Synchronized
    fun cleanupOldConfigs(ttlMs: Long) {
        try {
            val allPrefs = preferences.all
            val currentTime = Calendar.getInstance().timeInMillis
            val oldestValidDateMs = currentTime - ttlMs
            val editor = preferences.edit()
            
            // Find and remove expired user-specific configs
            allPrefs.keys
                .filter { it.startsWith(ConfigKeyPrefix) && !it.endsWith(FetchDateSuffix) }
                .forEach { key ->
                    val fetchDateKey = key + FetchDateSuffix
                    val fetchDate = preferences.getLong(fetchDateKey, 0)
                    if (fetchDate < oldestValidDateMs) {
                        editor.remove(key)
                        editor.remove(fetchDateKey)
                        DevCycleLogger.d("Removed expired config for key: $key")
                    }
                }
            
            // Check legacy keys as well
            val legacyKeys = listOf(IdentifiedConfigKey, AnonymousConfigKey)
            for (key in legacyKeys) {
                val fetchDateMs = preferences.getLong("$key.FETCH_DATE", 0)
                if (fetchDateMs < oldestValidDateMs) {
                    editor.remove(key)
                    editor.remove("$key.USER_ID")
                    editor.remove("$key.FETCH_DATE")
                    DevCycleLogger.d("Removed expired legacy config for key: $key")
                }
            }
            
            editor.apply()
        } catch (e: Exception) {
            DevCycleLogger.e(e, "Error cleaning up old configs: ${e.message}")
        }
    }
}