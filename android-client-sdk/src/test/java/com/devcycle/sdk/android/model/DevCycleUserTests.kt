package com.devcycle.sdk.android.model

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.devcycle.sdk.android.api.DevCycleClient
import com.devcycle.sdk.android.helpers.TestDVCLogger
import com.devcycle.sdk.android.util.DVCSharedPrefs
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import java.util.*

class DevCycleUserTests {

    private val logger = TestDVCLogger()
    private val mockApplication: Application = mock(Application::class.java)
    private val mockContext: Context = mock(Context::class.java)
    private val mockSharedPreferences: SharedPreferences = mock(SharedPreferences::class.java)
    private val mockEditor: SharedPreferences.Editor = mock(SharedPreferences.Editor::class.java)
    private val mockResources: Resources = mock(Resources::class.java)
    private val mockConfiguration: Configuration = mock(Configuration::class.java)
    private val mockLocaleList: LocaleList = mock(LocaleList::class.java)
    private val mockPackageManager: PackageManager = mock(PackageManager::class.java)
    private val mockPackageInfo = mockk<PackageInfo>()

    @BeforeEach
    fun setUp() {
        // Use the exact same pattern as DevCycleClientTests.kt
        `when`(mockContext.getString(anyInt())).thenReturn("Some value")
        `when`(mockContext.getSharedPreferences("Some value", MODE_PRIVATE)).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockContext.resources).thenReturn(mockResources)
        `when`(mockContext.resources.configuration).thenReturn(mockConfiguration)
        `when`(mockContext.resources.configuration.locales).thenReturn(mockLocaleList)
        `when`(mockContext.resources.configuration.locales.get(0)).thenReturn(Locale.getDefault())
        `when`(mockContext.packageName).thenReturn("test")
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.packageManager.getPackageInfo("test", 0)).thenReturn(mockPackageInfo)
        `when`(mockContext.applicationContext).thenReturn(mockApplication)

        // Use MockK for PackageInfo to handle longVersionCode properly
        every { mockPackageInfo.longVersionCode } returns 1

        // Additional SharedPreferences setup
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }
        `when`(mockEditor.commit()).thenReturn(true)
        `when`(mockSharedPreferences.getString(anyString(), anyString())).thenReturn(null)
        `when`(mockSharedPreferences.getLong(anyString(), anyLong())).thenReturn(0L)
        `when`(mockSharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        `when`(mockSharedPreferences.all).thenReturn(emptyMap())
        `when`(mockSharedPreferences.contains(anyString())).thenReturn(false)
    }

    @Test
    fun `validateDevCycleUser throws exception when isAnonymous is false but no userId provided`() {
        val user = DevCycleUser.builder()
            .withIsAnonymous(false)
            .build()

        val exception = assertThrows<IllegalArgumentException> {
            DevCycleClient.builder()
                .withContext(mockContext)
                .withSDKKey("test-sdk-key")
                .withUser(user)
                .withLogger(logger)
                .build()
        }

        Assertions.assertEquals("User ID is required when isAnonymous is false", exception.message)
    }

    @Test
    fun `validateDevCycleUser throws exception when isAnonymous is false with empty userId provided`() {
        val user = DevCycleUser.builder()
            .withIsAnonymous(false)
            .withUserId("")
            .build()

        val exception = assertThrows<IllegalArgumentException> {
            DevCycleClient.builder()
                .withContext(mockContext)
                .withSDKKey("test-sdk-key")
                .withUser(user)
                .withLogger(logger)
                .build()
        }

        Assertions.assertEquals("User ID is required when isAnonymous is false", exception.message)
    }

    @Test
    fun `validateDevCycleUser generates anonymous ID when no userId and isAnonymous is true`() {
        // Mock that no existing anonymous ID exists
        `when`(mockSharedPreferences.getString(DVCSharedPrefs.AnonUserIdKey, null)).thenReturn(null)

        val user = DevCycleUser.builder()
            .withIsAnonymous(true)
            .build()

        DevCycleClient.builder()
            .withContext(mockContext)
            .withSDKKey("test-sdk-key")
            .withUser(user)
            .withLogger(logger)
            .build()

        // Verify the user now has an anonymous ID and isAnonymous is true
        Assertions.assertEquals(true, user.isAnonymous)
        Assertions.assertNotNull(user.userId)
        // Verify anonymous ID was saved to shared preferences
        verify(mockEditor, atLeastOnce()).putString(eq(DVCSharedPrefs.AnonUserIdKey), anyString())
    }

    @Test
    fun `validateDevCycleUser generates anonymous ID when no userId and isAnonymous not set`() {
        // Mock that no existing anonymous ID exists
        `when`(mockSharedPreferences.getString(DVCSharedPrefs.AnonUserIdKey, null)).thenReturn(null)

        val user = DevCycleUser.builder().build()

        DevCycleClient.builder()
            .withContext(mockContext)
            .withSDKKey("test-sdk-key")
            .withUser(user)
            .withLogger(logger)
            .build()

        // Verify the user now has generated anonymous ID and isAnonymous is set to true
        Assertions.assertEquals(true, user.isAnonymous)
        Assertions.assertNotNull(user.userId)
        // Verify anonymous ID was saved to shared preferences
        verify(mockEditor, atLeastOnce()).putString(eq(DVCSharedPrefs.AnonUserIdKey), anyString())
    }

    @Test
    fun `validateDevCycleUser generates anonymous ID when empty userId string and no isAnonymous set`() {
        // Mock that no existing anonymous ID exists
        `when`(mockSharedPreferences.getString(DVCSharedPrefs.AnonUserIdKey, null)).thenReturn(null)

        val user = DevCycleUser.builder().withUserId("").build()

        DevCycleClient.builder()
            .withContext(mockContext)
            .withSDKKey("test-sdk-key")
            .withUser(user)
            .withLogger(logger)
            .build()

        // Verify the user now has generated anonymous ID and isAnonymous is set to true
        Assertions.assertEquals(true, user.isAnonymous)
        Assertions.assertNotNull(user.userId)
        // Verify anonymous ID was saved to shared preferences
        verify(mockEditor, atLeastOnce()).putString(eq(DVCSharedPrefs.AnonUserIdKey), anyString())
    }

    @Test
    fun `validateDevCycleUser uses existing anonymous ID when available`() {
        val existingAnonId = "existing-anon-id"
        `when`(mockSharedPreferences.getString(DVCSharedPrefs.AnonUserIdKey, null)).thenReturn(existingAnonId)

        val user = DevCycleUser.builder()
            .withIsAnonymous(true)
            .build()

        DevCycleClient.builder()
            .withContext(mockContext)
            .withSDKKey("test-sdk-key")
            .withUser(user)
            .withLogger(logger)
            .build()

        // Verify the user gets the existing anonymous ID
        Assertions.assertEquals(true, user.isAnonymous)
        Assertions.assertEquals(existingAnonId, user.userId)
        // Verify no new anonymous ID was generated (putString should not be called for AnonUserIdKey)
        verify(mockEditor, never()).putString(eq(DVCSharedPrefs.AnonUserIdKey), anyString())
    }

    @Test
    fun `validateDevCycleUser sets isAnonymous to false when valid userId provided`() {
        val user = DevCycleUser.builder()
            .withUserId("valid-user-id")
            .build()

        DevCycleClient.builder()
            .withContext(mockContext)
            .withSDKKey("test-sdk-key")
            .withUser(user)
            .withLogger(logger)
            .build()

        // Verify isAnonymous is set to false for identified users
        Assertions.assertEquals(false, user.isAnonymous)
        Assertions.assertEquals("valid-user-id", user.userId)
    }

    @Test
    fun `validateDevCycleUser overrides isAnonymous to false when valid userId provided even if originally true`() {
        val user = DevCycleUser.builder()
            .withUserId("valid-user-id")
            .withIsAnonymous(true)
            .build()

        DevCycleClient.builder()
            .withContext(mockContext)
            .withSDKKey("test-sdk-key")
            .withUser(user)
            .withLogger(logger)
            .build()

        // Verify isAnonymous is overridden to false when valid userId is provided
        Assertions.assertEquals(false, user.isAnonymous)
        Assertions.assertEquals("valid-user-id", user.userId)
    }

    @Test
    fun `internal setters can modify immutable properties`() {
        val user = DevCycleUser.builder()
            .withUserId("original_id")
            .withIsAnonymous(false)
            .build()

        // Verify original values
        Assertions.assertEquals("original_id", user.userId)
        Assertions.assertEquals(false, user.isAnonymous)

        // Modify using internal setters
        user.setUserId("new_id")
        user.setIsAnonymous(true)

        // Verify modified values
        Assertions.assertEquals("new_id", user.userId)
        Assertions.assertEquals(true, user.isAnonymous)
    }
}
