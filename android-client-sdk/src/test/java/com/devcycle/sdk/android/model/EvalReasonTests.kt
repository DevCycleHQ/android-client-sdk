package com.devcycle.sdk.android.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvalReasonTests {

    @Test
    fun `defaultReason creates EvalReason with DEFAULT reason and given details`() {
        val evalReason = EvalReason.defaultReason("User Not Targeted")

        assertEquals("DEFAULT", evalReason.reason)
        assertEquals("User Not Targeted", evalReason.details)
        assertNull(evalReason.targetId)
    }

}
