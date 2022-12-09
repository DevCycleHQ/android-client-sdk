package com.devcycle.sdk.android.util

import android.content.Context
import android.content.SharedPreferences
import com.devcycle.sdk.android.R
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.PopulatedUser
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import timber.log.Timber
import java.util.Calendar

// TODO: access disk on background thread
internal class DVCSharedPrefs(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.cached_data),
        Context.MODE_PRIVATE
    )
    private val objectMapper = jacksonObjectMapper()

    companion object {
        const val UserKey = "USER"
        const val AnonUserIdKey = "ANONYMOUS_USER_ID"
        const val IdentifiedConfigKey = "IDENTIFIED_CONFIG"
        const val AnonymousConfigKey = "ANONYMOUS_CONFIG"
        private val prefs: MutableMap<String, TypeReference<*>> = HashMap()

        init {
            prefs[UserKey] = object : TypeReference<PopulatedUser?>() {}
            prefs[IdentifiedConfigKey] = object : TypeReference<BucketedUserConfig?>() {}
            prefs[AnonymousConfigKey] = object : TypeReference<BucketedUserConfig?>() {}
        }
    }

    @Synchronized
    fun <T> save(objectToSave: T, key: String?) {
        try {
            val jsonString = objectMapper.writeValueAsString(objectToSave)
            preferences.edit().putString(key, jsonString).apply()
        } catch (e: JsonProcessingException) {
            Timber.e(e, e.message)
        }
    }

    @Synchronized
    fun remove(key: String?) {
        try {
            val editor: SharedPreferences.Editor = preferences.edit()
            editor.remove(key)
            editor.commit()
        } catch (e: JsonProcessingException) {
            Timber.e(e, e.message)
        }
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T> getCache(key: String): T? {
        val jsonString = preferences.getString(key, null)
        if (jsonString == null) {
            Timber.e("%s could not be found in SharedPreferences file: %s", key, R.string.cached_data)
            return null
        }
        try {
            return objectMapper.readValue(jsonString, prefs[key]) as T
        } catch (e: JsonProcessingException) {
            Timber.e(e, e.message)
        }
        return null
    }

    fun getString(key: String): String? {
        val stringValue = preferences.getString(key,null)
        if (stringValue == null) {
            Timber.i("%s could not be found in SharedPreferences file: %s", key, R.string.cached_data)
            return null
        }
        return stringValue
    }

    @Synchronized
    fun saveString(value: String, key: String) {
        try {
            preferences.edit().putString(key, value).apply()
            preferences.edit().commit()
        } catch (e: JsonProcessingException) {
            Timber.e(e, e.message)
        }
    }

    @Synchronized
    fun saveConfig(configToSave: BucketedUserConfig, user: PopulatedUser) {
        try {
            val key = if (user.isAnonymous) AnonymousConfigKey else IdentifiedConfigKey
            val editor = preferences.edit()
            val jsonString = objectMapper.writeValueAsString(configToSave)
            editor.putString(key, jsonString)
            editor.putString("$key.USER_ID", user.userId)
            editor.putLong("$key.FETCH_DATE", Calendar.getInstance().timeInMillis)
            editor.apply()
        } catch (e: JsonProcessingException) {
            Timber.e(e, e.message)
        }
    }

    @Synchronized
    fun getConfig(user: PopulatedUser, ttlMs: Long): BucketedUserConfig? {
        try {
            val key = if (user.isAnonymous) AnonymousConfigKey else IdentifiedConfigKey
            val userId = preferences.getString("$key.USER_ID", null)
            val fetchDateMs = preferences.getLong("$key.FETCH_DATE", 0)

            if (userId != user.userId) {
                Timber.d("Skipping cached config: no config for user ID ${user.userId}")
                return null
            }

            val oldestValidDateMs = Calendar.getInstance().timeInMillis - ttlMs
            if (fetchDateMs < oldestValidDateMs) {
                Timber.d("Skipping cached config: last fetched date is too old")
                return null
            }

            val configString = preferences.getString(key, null)
            if (configString == null) {
                Timber.d("Skipping cached config: no config found")
                return null
            }
            return objectMapper.readValue(configString, prefs[key]) as BucketedUserConfig?
        } catch (e: JsonProcessingException) {
            Timber.e(e, e.message)
            return null
        }
    }

    init {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}