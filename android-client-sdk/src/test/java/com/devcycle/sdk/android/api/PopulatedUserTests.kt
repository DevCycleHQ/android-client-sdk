package com.devcycle.sdk.android.api

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.LocaleList
import com.devcycle.sdk.android.model.DevCycleUser
import com.devcycle.sdk.android.model.PopulatedUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.Locale

class PopulatedUserTests {
    private val mockContext: Context? = Mockito.mock(Context::class.java)
    private val sharedPreferences: SharedPreferences? = Mockito.mock(SharedPreferences::class.java)

    private val editor: SharedPreferences.Editor? =
        Mockito.mock(SharedPreferences.Editor::class.java)
    private val resources: Resources = Mockito.mock(Resources::class.java)
    private val configuration: Configuration = Mockito.mock(Configuration::class.java)
    private val locales: LocaleList = Mockito.mock(LocaleList::class.java)
    private val packageManager: PackageManager = Mockito.mock(PackageManager::class.java)
    private val mockHandler: Handler = Mockito.mock(Handler::class.java)
    private val mockApplication: Application? = Mockito.mock(Application::class.java)
    private val packageInfo = PackageInfo()

    @BeforeEach
    fun setup() {
        Mockito.`when`(mockContext!!.getString(ArgumentMatchers.anyInt())).thenReturn("Some value")
        Mockito.`when`(mockContext.getSharedPreferences("Some value", Context.MODE_PRIVATE))
            .thenReturn(
                sharedPreferences
            )
        Mockito.`when`(sharedPreferences!!.edit()).thenReturn(editor)
        Mockito.`when`(
            editor!!.putString(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(editor)
        Mockito.`when`(mockContext.resources).thenReturn(resources)
        Mockito.`when`(mockContext.resources.configuration).thenReturn(configuration)
        Mockito.`when`(mockContext.resources.configuration.locales).thenReturn(locales)
        Mockito.`when`(mockContext.resources.configuration.locales.get(0))
            .thenReturn(Locale.getDefault())
        Mockito.`when`(mockContext.packageName).thenReturn("test")
        Mockito.`when`(mockContext.packageManager).thenReturn(packageManager)

        Mockito.`when`(mockContext.packageManager.getPackageInfo("test", 0)).thenReturn(packageInfo)
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockApplication)
    }

    @Test
    fun `should create PopulatedUser with defaults if context resources is null`() {
        Mockito.`when`(mockContext?.resources).thenReturn(null, resources)
        val dvcUser = DevCycleUser.builder().withUserId("User1").build()
        val populatedUser = PopulatedUser.fromUserParam(dvcUser!!, mockContext!!)

        assert(populatedUser.language == "en")
    }

    @Test
    fun `should create PopulatedUser with proper language`() {
        val locale = Locale("fr")
        Mockito.`when`(mockContext?.resources?.configuration?.locales?.get(0))
            .thenReturn(locale)
        val dvcUser = DevCycleUser.builder().withUserId("User1").build()
        val populatedUser = PopulatedUser.fromUserParam(dvcUser!!, mockContext!!)

        assert(populatedUser.language == "fr")
    }

    @Test
    fun `should create PopulatedUser with defaults if packageManager code is null`() {
        Mockito.`when`(mockContext?.packageManager).thenReturn(null, packageManager)
        val dvcUser = DevCycleUser.builder().withUserId("User1").build()
        val populatedUser = PopulatedUser.fromUserParam(dvcUser!!, mockContext!!)

        assert(populatedUser.appVersion == "unknown")
        assert(populatedUser.appBuild == 0L)
    }
}