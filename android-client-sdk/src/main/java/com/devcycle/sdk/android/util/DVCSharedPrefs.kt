package com.devcycle.sdk.android.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.devcycle.sdk.android.R
import com.fasterxml.jackson.databind.ObjectMapper
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.fasterxml.jackson.core.JsonProcessingException
import com.devcycle.sdk.android.model.User
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import java.util.HashMap

class DVCSharedPrefs(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences(
        context.getString(R.string.cached_data),
        Context.MODE_PRIVATE
    )
    private val objectMapper: ObjectMapper = ObjectMapper()

    companion object {
        private val TAG = DVCSharedPrefs::class.simpleName
        const val UserKey = "USER"
        const val ConfigKey = "CONFIG"
        private val prefs: MutableMap<String, TypeReference<*>> = HashMap()

        init {
            prefs[UserKey] = object : TypeReference<User?>() {}
            prefs[ConfigKey] = object : TypeReference<BucketedUserConfig?>() {}
        }
    }

    fun <T> save(objectToSave: T, key: String?) {
        try {
            val jsonString = objectMapper.writeValueAsString(objectToSave)
            preferences.edit().putString(key, jsonString).apply()
        } catch (e: JsonProcessingException) {
            Log.e(TAG, e.message, e)
        }
    }

    fun <T> getCache(key: String): T? {
        val jsonString = preferences.getString(key, null)
        if (jsonString == null) {
            Log.e(
                TAG,
                key + " could not be found in SharedPreferences file: " + R.string.cached_data
            )
        }
        try {
            return objectMapper.readValue(jsonString, prefs[key]) as T
        } catch (e: JsonProcessingException) {
            Log.e(TAG, e.message, e)
        }
        return null
    }

    init {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}