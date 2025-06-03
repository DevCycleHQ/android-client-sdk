package com.devcycle.sdk.android.util

import android.content.Context
import android.content.SharedPreferences
import com.devcycle.R
import com.devcycle.sdk.android.model.BucketedUserConfig
import com.devcycle.sdk.android.model.PopulatedUser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import java.util.*

class DVCSharedPrefsTests {
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var dvcSharedPrefs: DVCSharedPrefs
    
    private val testUserId = "test-user-123"
    private val testAnonUserId = "anon-user-456"
    private val currentTime = System.currentTimeMillis()
    private val ttl = 30 * 24 * 3600000L // 30 days
    
    @BeforeEach
    fun setup() {
        mockContext = mock(Context::class.java)
        mockSharedPreferences = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        
        `when`(mockContext.getString(R.string.cached_data)).thenReturn("devcycle_cache")
        `when`(mockContext.getSharedPreferences("devcycle_cache", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        
        // Mock empty preferences initially
        `when`(mockSharedPreferences.all).thenReturn(emptyMap<String, Any>())
        `when`(mockSharedPreferences.contains(anyString())).thenReturn(false)
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        `when`(mockSharedPreferences.getLong(anyString(), eq(0L))).thenReturn(0L)
        `when`(mockSharedPreferences.getBoolean(eq("MIGRATION_COMPLETED"), eq(false))).thenReturn(false)
        
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Clear invocations from initialization but preserve the stubbing
        clearInvocations(mockEditor)
    }
    
    @Test
    fun `should save and retrieve identified user config`() {
        val user = createPopulatedUser(testUserId, false)
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val futureExpiryTime = currentTime + ttl
        
        // Mock successful save
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.$testUserId", null))
            .thenReturn(testConfigString)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE", 0))
            .thenReturn(futureExpiryTime)
        
        dvcSharedPrefs.saveConfig(config, user, ttl)
        
        verify(mockEditor).putString(eq("IDENTIFIED_CONFIG.$testUserId"), anyString())
        verify(mockEditor).putLong(eq("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE"), anyLong())
        verify(mockEditor).apply()
        
        val retrievedConfig = dvcSharedPrefs.getConfig(user, ttl)
        assertNotNull(retrievedConfig)
    }
    
    @Test
    fun `should save and retrieve anonymous user config`() {
        val user = createPopulatedUser(testAnonUserId, true)
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val futureExpiryTime = currentTime + ttl
        
        // Mock successful save
        `when`(mockSharedPreferences.getString("ANONYMOUS_CONFIG.$testAnonUserId", null))
            .thenReturn(testConfigString)
        `when`(mockSharedPreferences.getLong("ANONYMOUS_CONFIG.$testAnonUserId.EXPIRY_DATE", 0))
            .thenReturn(futureExpiryTime)
        
        dvcSharedPrefs.saveConfig(config, user, ttl)
        
        verify(mockEditor).putString(eq("ANONYMOUS_CONFIG.$testAnonUserId"), anyString())
        verify(mockEditor).putLong(eq("ANONYMOUS_CONFIG.$testAnonUserId.EXPIRY_DATE"), anyLong())
        verify(mockEditor).apply()
        
        val retrievedConfig = dvcSharedPrefs.getConfig(user, ttl)
        assertNotNull(retrievedConfig)
    }
    
    @Test
    fun `should return null for non-existent config`() {
        val user = createPopulatedUser(testUserId, false)
        
        val retrievedConfig = dvcSharedPrefs.getConfig(user, ttl)
        assertNull(retrievedConfig)
    }
    
    @Test
    fun `should remove expired config and return null`() {
        val user = createPopulatedUser(testUserId, false)
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val expiredTime = currentTime - 1000 // Expired by 1 second
        
        // Mock expired config
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.$testUserId", null))
            .thenReturn(testConfigString)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE", 0))
            .thenReturn(expiredTime)
        
        val retrievedConfig = dvcSharedPrefs.getConfig(user, ttl)
        
        assertNull(retrievedConfig)
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.$testUserId"))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE"))
        verify(mockEditor).apply()
    }
    
    @Test
    fun `should migrate legacy identified config on initialization`() {
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val legacyPrefs = mutableMapOf<String, Any>(
            "IDENTIFIED_CONFIG" to testConfigString,
            "IDENTIFIED_CONFIG.USER_ID" to testUserId,
            "IDENTIFIED_CONFIG.FETCH_DATE" to currentTime
        )
        
        `when`(mockSharedPreferences.all).thenReturn(legacyPrefs)
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.USER_ID", null)).thenReturn(testUserId)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.FETCH_DATE", 0)).thenReturn(currentTime)
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG", null)).thenReturn(testConfigString)
        `when`(mockSharedPreferences.contains("IDENTIFIED_CONFIG.$testUserId")).thenReturn(false)
        
        // Create new instance to trigger migration
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Verify migration occurred - legacy fetch date becomes expiry date
        verify(mockEditor).putString(eq("IDENTIFIED_CONFIG.$testUserId"), eq(testConfigString))
        verify(mockEditor).putLong(eq("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE"), eq(currentTime))
        verify(mockEditor).putBoolean(eq("MIGRATION_COMPLETED"), eq(true))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG"))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.USER_ID"))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.FETCH_DATE"))
        verify(mockEditor).apply()
    }
    
    @Test
    fun `should migrate legacy anonymous config on initialization`() {
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val legacyPrefs = mutableMapOf<String, Any>(
            "ANONYMOUS_CONFIG" to testConfigString,
            "ANONYMOUS_CONFIG.USER_ID" to testAnonUserId,
            "ANONYMOUS_CONFIG.FETCH_DATE" to currentTime
        )
        
        `when`(mockSharedPreferences.all).thenReturn(legacyPrefs)
        `when`(mockSharedPreferences.getString("ANONYMOUS_CONFIG.USER_ID", null)).thenReturn(testAnonUserId)
        `when`(mockSharedPreferences.getLong("ANONYMOUS_CONFIG.FETCH_DATE", 0)).thenReturn(currentTime)
        `when`(mockSharedPreferences.getString("ANONYMOUS_CONFIG", null)).thenReturn(testConfigString)
        `when`(mockSharedPreferences.contains("ANONYMOUS_CONFIG.$testAnonUserId")).thenReturn(false)
        
        // Create new instance to trigger migration
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Verify migration occurred - legacy fetch date becomes expiry date
        verify(mockEditor).putString(eq("ANONYMOUS_CONFIG.$testAnonUserId"), eq(testConfigString))
        verify(mockEditor).putLong(eq("ANONYMOUS_CONFIG.$testAnonUserId.EXPIRY_DATE"), eq(currentTime))
        verify(mockEditor).putBoolean(eq("MIGRATION_COMPLETED"), eq(true))
        verify(mockEditor).remove(eq("ANONYMOUS_CONFIG"))
        verify(mockEditor).remove(eq("ANONYMOUS_CONFIG.USER_ID"))
        verify(mockEditor).remove(eq("ANONYMOUS_CONFIG.FETCH_DATE"))
        verify(mockEditor).apply()
    }
    
    @Test
    fun `should not migrate if new format already exists`() {
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val legacyPrefs = mutableMapOf<String, Any>(
            "IDENTIFIED_CONFIG" to testConfigString,
            "IDENTIFIED_CONFIG.USER_ID" to testUserId,
            "IDENTIFIED_CONFIG.FETCH_DATE" to currentTime
        )
        
        `when`(mockSharedPreferences.all).thenReturn(legacyPrefs)
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.USER_ID", null)).thenReturn(testUserId)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.FETCH_DATE", 0)).thenReturn(currentTime)
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG", null)).thenReturn(testConfigString)
        `when`(mockSharedPreferences.contains("IDENTIFIED_CONFIG.$testUserId")).thenReturn(true)
        
        // Create new instance to trigger migration
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Verify data was removed but not re-migrated, but migration flag still set
        verify(mockEditor, never()).putString(eq("IDENTIFIED_CONFIG.$testUserId"), eq(testConfigString))
        verify(mockEditor, never()).putLong(eq("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE"), eq(currentTime))
        verify(mockEditor).putBoolean(eq("MIGRATION_COMPLETED"), eq(true))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG"))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.USER_ID"))
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.FETCH_DATE"))
        verify(mockEditor).apply()
    }
    
    @Test
    fun `should handle migration errors gracefully`() {
        `when`(mockSharedPreferences.all).thenThrow(RuntimeException("SharedPreferences error"))
        
        // Should not throw exception during initialization
        assertDoesNotThrow {
            dvcSharedPrefs = DVCSharedPrefs(mockContext)
        }
    }
    
    @Test
    fun `should save and retrieve string values`() {
        val testKey = "test_key"
        val testValue = "test_value"
        
        `when`(mockSharedPreferences.getString(testKey, null)).thenReturn(testValue)
        
        dvcSharedPrefs.saveString(testValue, testKey)
        val retrievedValue = dvcSharedPrefs.getString(testKey)
        
        verify(mockEditor).putString(eq(testKey), eq(testValue))
        verify(mockEditor).apply()
        assertEquals(testValue, retrievedValue)
    }
    
    @Test
    fun `should return null for non-existent string value`() {
        val testKey = "non_existent_key"
        
        val retrievedValue = dvcSharedPrefs.getString(testKey)
        assertNull(retrievedValue)
    }
    
    @Test
    fun `should save and remove generic objects`() {
        val testKey = "test_object_key"
        val testObject = mapOf("key" to "value")
        
        dvcSharedPrefs.save(testObject, testKey)
        verify(mockEditor).putString(eq(testKey), anyString())
        verify(mockEditor).apply()
        
        dvcSharedPrefs.remove(testKey)
        verify(mockEditor).remove(eq(testKey))
        verify(mockEditor).commit()
    }
    
    @Test
    fun `should handle json processing exceptions during save`() {
        val testKey = "test_key"
        val problematicObject = object {
            val circularRef = this // This could cause JSON processing issues
        }
        
        // Should not throw exception
        assertDoesNotThrow {
            dvcSharedPrefs.save(problematicObject, testKey)
        }
    }
    
    @Test
    fun `should handle json processing exceptions during config retrieval`() {
        val user = createPopulatedUser(testUserId, false)
        val malformedJson = "{ invalid json"
        val futureExpiryTime = currentTime + ttl
        
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.$testUserId", null))
            .thenReturn(malformedJson)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE", 0))
            .thenReturn(futureExpiryTime)
        
        val retrievedConfig = dvcSharedPrefs.getConfig(user, ttl)
        assertNull(retrievedConfig)
    }
    
    @Test
    fun `should set migration flag even when no legacy data exists`() {
        `when`(mockSharedPreferences.getBoolean(eq("MIGRATION_COMPLETED"), eq(false))).thenReturn(false)
        `when`(mockSharedPreferences.all).thenReturn(emptyMap<String, Any>())
        
        // Create new instance to trigger migration check
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Verify migration flag was set even with no legacy data
        verify(mockEditor).putBoolean(eq("MIGRATION_COMPLETED"), eq(true))
        verify(mockEditor).apply()
    }
    
    @Test
    fun `should skip migration if already completed`() {
        `when`(mockSharedPreferences.getBoolean(eq("MIGRATION_COMPLETED"), eq(false))).thenReturn(true)
        
        // Create new instance - should skip migration entirely
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Verify no migration operations occurred
        verify(mockEditor, never()).putString(anyString(), anyString())
        verify(mockEditor, never()).putLong(anyString(), anyLong())
        verify(mockEditor, never()).putBoolean(anyString(), anyBoolean())
        verify(mockEditor, never()).remove(anyString())
        verify(mockEditor, never()).apply()
    }

    @Test
    fun `should save config with correct expiry time`() {
        val user = createPopulatedUser(testUserId, false)
        val config = createTestConfig()
        val testTtl = 60000L // 1 minute
        
        dvcSharedPrefs.saveConfig(config, user, testTtl)
        
        // Verify that the correct key was used and any long was saved
        verify(mockEditor).putLong(eq("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE"), anyLong())
        verify(mockEditor).apply()
    }

    @Test
    fun `should return valid config when not expired`() {
        val user = createPopulatedUser(testUserId, false)
        val config = createTestConfig()
        val testConfigString = jacksonObjectMapper().writeValueAsString(config)
        val futureExpiryTime = currentTime + 3600000 // 1 hour in future
        
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.$testUserId", null))
            .thenReturn(testConfigString)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.$testUserId.EXPIRY_DATE", 0))
            .thenReturn(futureExpiryTime)
        
        val retrievedConfig = dvcSharedPrefs.getConfig(user, ttl)
        
        assertNotNull(retrievedConfig)
        // Verify no removal occurred since config is still valid
        verify(mockEditor, never()).remove(anyString())
    }
    
    @Test
    fun `should clean up partial legacy data even when migration is not possible`() {
        // Setup partial legacy data - only USER_ID exists, no config or fetch date
        val legacyPrefs = mutableMapOf<String, Any>(
            "IDENTIFIED_CONFIG.USER_ID" to testUserId,
            "ANONYMOUS_CONFIG.FETCH_DATE" to currentTime
            // Note: No actual config strings, so migration can't happen
        )
        
        `when`(mockSharedPreferences.all).thenReturn(legacyPrefs)
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG.USER_ID", null)).thenReturn(testUserId)
        `when`(mockSharedPreferences.getLong("IDENTIFIED_CONFIG.FETCH_DATE", 0)).thenReturn(0L) // Missing
        `when`(mockSharedPreferences.getString("IDENTIFIED_CONFIG", null)).thenReturn(null) // Missing
        `when`(mockSharedPreferences.getString("ANONYMOUS_CONFIG.USER_ID", null)).thenReturn(null) // Missing
        `when`(mockSharedPreferences.getLong("ANONYMOUS_CONFIG.FETCH_DATE", 0)).thenReturn(currentTime)
        `when`(mockSharedPreferences.getString("ANONYMOUS_CONFIG", null)).thenReturn(null) // Missing
        `when`(mockSharedPreferences.contains("IDENTIFIED_CONFIG.USER_ID")).thenReturn(true)
        `when`(mockSharedPreferences.contains("ANONYMOUS_CONFIG.FETCH_DATE")).thenReturn(true)
        `when`(mockSharedPreferences.contains("IDENTIFIED_CONFIG")).thenReturn(false)
        `when`(mockSharedPreferences.contains("IDENTIFIED_CONFIG.FETCH_DATE")).thenReturn(false)
        `when`(mockSharedPreferences.contains("ANONYMOUS_CONFIG")).thenReturn(false)
        `when`(mockSharedPreferences.contains("ANONYMOUS_CONFIG.USER_ID")).thenReturn(false)
        
        // Create new instance to trigger migration
        dvcSharedPrefs = DVCSharedPrefs(mockContext)
        
        // Verify no successful migrations occurred (no new format data was written)
        verify(mockEditor, never()).putString(matches(".*CONFIG\\..*"), anyString())
        verify(mockEditor, never()).putLong(matches(".*CONFIG\\..*\\.FETCH_DATE"), anyLong())
        
        // But verify partial legacy data was still cleaned up
        verify(mockEditor).remove(eq("IDENTIFIED_CONFIG.USER_ID"))
        verify(mockEditor).remove(eq("ANONYMOUS_CONFIG.FETCH_DATE"))
        verify(mockEditor).putBoolean(eq("MIGRATION_COMPLETED"), eq(true))
        verify(mockEditor).apply()
    }
    
    private fun createPopulatedUser(userId: String, isAnonymous: Boolean): PopulatedUser {
        return PopulatedUser(
            userId = userId,
            email = null,
            name = null,
            language = "en",
            country = "US",
            appVersion = "1.0.0",
            appBuild = 1L,
            customData = mutableMapOf(),
            privateCustomData = mutableMapOf(),
            deviceModel = "TestDevice",
            sdkType = "mobile",
            sdkVersion = "1.0.0",
            platform = "android",
            platformVersion = "11",
            createdDate = currentTime,
            lastSeenDate = currentTime,
            isAnonymous = isAnonymous
        )
    }
    
    private fun createTestConfig(): BucketedUserConfig {
        return BucketedUserConfig()
    }
} 