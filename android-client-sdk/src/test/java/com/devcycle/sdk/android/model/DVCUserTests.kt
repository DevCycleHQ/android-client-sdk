package com.devcycle.sdk.android.model

import org.junit.jupiter.api.*


class DVCUserTests {

    @Test
    fun `DVCUser has isAnonymous=false when user id is set`() {
        val user = DVCUser.builder().withUserId("new_userid").build()

        Assertions.assertEquals(false, user.isAnonymous)
    }

    @Test
    fun `DVCUser has isAnonymous=true when user id is not set`() {
        val user = DVCUser.builder().build()

        Assertions.assertEquals(true, user.isAnonymous)
    }
}
