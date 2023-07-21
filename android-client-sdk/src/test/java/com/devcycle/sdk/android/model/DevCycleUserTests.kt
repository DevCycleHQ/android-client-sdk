package com.devcycle.sdk.android.model

import org.junit.jupiter.api.*


class DevCycleUserTests {

    @Test
    fun `DevCycleUser has isAnonymous=false when user id is set`() {
        val user = DevCycleUser.builder().withUserId("new_userid").build()

        Assertions.assertEquals(false, user.isAnonymous)
    }

    @Test
    fun `DevCycleUser has isAnonymous=true when user id is not set`() {
        val user = DevCycleUser.builder().build()

        Assertions.assertEquals(true, user.isAnonymous)
    }
}
