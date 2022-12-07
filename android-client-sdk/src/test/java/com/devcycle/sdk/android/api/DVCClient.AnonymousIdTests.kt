package com.devcycle.sdk.android.api

import com.devcycle.sdk.android.model.*
import org.junit.jupiter.api.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*


class AnonymousIdTests : DVCClientTestBase() {
    @Test
    fun `client uses stored anonymous id if it exists`() {
        `when`(sharedPreferences?.getString(eq("ANONYMOUS_USER_ID"), eq(null))).thenReturn("some-anon-id")

        val user = DVCClient::class.java.getDeclaredField("user")
        user.setAccessible(true)

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        var anonUser: PopulatedUser = user.get(client) as PopulatedUser

        Assertions.assertEquals("some-anon-id", anonUser.userId)
    }

    @Test
    fun `client writes anonymous id to store if it doesn't exist`() {
        val user = DVCClient::class.java.getDeclaredField("user")
        user.setAccessible(true)

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        var anonUser: PopulatedUser = user.get(client) as PopulatedUser

        verify(editor, times(1))?.putString(eq("ANONYMOUS_USER_ID"), eq(anonUser.userId))
    }

    @Test
    fun `identifying a user clears the stored anonymous id`() {
        `when`(sharedPreferences?.getString(anyString(), eq(null))).thenReturn("some-anon-id")

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        val newUser = DVCUser.builder().withUserId("123").withIsAnonymous(false).build()
        val callback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                verify(editor, times(1))?.remove(eq("ANONYMOUS_USER_ID"))
            }
            override fun onError(t: Throwable) {}
        }
        client.identifyUser(newUser, callback)
    }

    @Test
    fun `resetting the user updates the stored anonymous id`() {
        `when`(sharedPreferences?.getString(anyString(), eq(null))).thenReturn("some-anon-id")
        val user = DVCClient::class.java.getDeclaredField("latestIdentifiedUser")
        user.setAccessible(true)

        val client = createClient(user=DVCUser.builder().withIsAnonymous(true).build())
        val callback = object: DVCCallback<Map<String, Variable<Any>>> {
            override fun onSuccess(result: Map<String, Variable<Any>>) {
                var anonUser: PopulatedUser = user.get(client) as PopulatedUser
                verify(editor, times(1))?.putString(eq("ANONYMOUS_USER_ID"), eq(anonUser.userId))
            }
            override fun onError(t: Throwable) {}
        }
        client.resetUser(callback)
    }
}

