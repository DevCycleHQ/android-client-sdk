package com.devcycle.sdk.android.util

import android.content.Context
import android.content.SharedPreferences
import com.devcycle.sdk.android.R
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.User
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import timber.log.Timber

// TODO: access disk on background thread
class DVCSharedPrefs(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.cached_data),
        Context.MODE_PRIVATE
    )
    private val objectMapper = jacksonObjectMapper()

    companion object {
        const val UserKey = "USER"
        const val ConfigKey = "CONFIG"
        const val AnonUserIdKey = "ANONYMOUS_USER_ID"
        private val prefs: MutableMap<String, TypeReference<*>> = HashMap()

        init {
            prefs[UserKey] = object : TypeReference<User?>() {}
            prefs[ConfigKey] = object : TypeReference<BucketedUserConfig?>() {}
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

    init {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}