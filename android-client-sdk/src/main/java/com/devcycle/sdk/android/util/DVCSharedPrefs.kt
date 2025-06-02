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

    init {
        migrateLegacyConfigs()
    }

    companion object {
        const val UserKey = "USER"
        const val AnonUserIdKey = "ANONYMOUS_USER_ID"
        const val IdentifiedConfigKey = "IDENTIFIED_CONFIG"
        const val AnonymousConfigKey = "ANONYMOUS_CONFIG"
        const val FetchDateSuffix = "FETCH_DATE"
    }

    private fun generateUserConfigKey(userId: String, isAnonymous: Boolean): String {
        val prefix = if (isAnonymous) AnonymousConfigKey else IdentifiedConfigKey
        return "$prefix.$userId"
    }

    private fun generateUserFetchDateKey(userId: String, isAnonymous: Boolean): String {
        return "${generateUserConfigKey(userId, isAnonymous)}.$FetchDateSuffix"
    }

    @Synchronized
    private fun migrateLegacyConfigs() {
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

                if (userId != null && configString != null && fetchDateMs > 0) {
                    val isAnonymous = legacyKey == AnonymousConfigKey
                    val userKey = generateUserConfigKey(userId, isAnonymous)
                    val userFetchDateKey = generateUserFetchDateKey(userId, isAnonymous)
                    
                    // Only migrate if new format doesn't already exist
                    if (!preferences.contains(userKey)) {
                        editor.putString(userKey, configString)
                        editor.putLong(userFetchDateKey, fetchDateMs)
                        migrationOccurred = true
                        DevCycleLogger.d("Migrated legacy config for user ID $userId from key $legacyKey")
                    }
                    
                    // Remove legacy data
                    editor.remove(legacyKey)
                    editor.remove(legacyUserIdKey)
                    editor.remove(legacyFetchDateKey)
                    migrationOccurred = true
                }
            }

            if (migrationOccurred) {
                editor.apply()
                DevCycleLogger.d("Legacy config migration completed")
            }
        } catch (e: Exception) {
            DevCycleLogger.e(e, "Error during legacy config migration: ${e.message}")
        }
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
            val userKey = generateUserConfigKey(user.userId, user.isAnonymous)
            val userFetchDateKey = generateUserFetchDateKey(user.userId, user.isAnonymous)

            val editor = preferences.edit()
            val jsonString = JSONMapper.mapper.writeValueAsString(configToSave)
            editor.putString(userKey, jsonString)
            editor.putLong(userFetchDateKey, Calendar.getInstance().timeInMillis)
            editor.apply()
        } catch (e: JsonProcessingException) {
            DevCycleLogger.e(e, e.message)
        }
    }

    @Synchronized
    fun getConfig(user: PopulatedUser, ttlMs: Long): BucketedUserConfig? {
        try {
            val userKey = generateUserConfigKey(user.userId, user.isAnonymous)
            val userConfigString = preferences.getString(userKey, null)
            val userFetchDateKey = generateUserFetchDateKey(user.userId, user.isAnonymous)
            val userFetchDateMs = preferences.getLong(userFetchDateKey, 0)
            
            val oldestValidDateMs = Calendar.getInstance().timeInMillis - ttlMs
            
            if (userConfigString != null) {
                if (userFetchDateMs >= oldestValidDateMs) {
                    DevCycleLogger.d("Loaded config from cache for user ID ${user.userId}")
                    return JSONMapper.mapper.readValue(userConfigString)
                } else {
                    // Config exists but is expired, remove it
                    val editor = preferences.edit()
                    editor.remove(userKey)
                    editor.remove(userFetchDateKey)
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