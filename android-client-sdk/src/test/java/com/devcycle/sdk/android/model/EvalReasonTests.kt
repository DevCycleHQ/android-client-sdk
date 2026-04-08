package com.devcycle.sdk.android.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvalReasonTests {

    @Test
    fun `withCachedSource preserves original reason and sets source to CACHED`() {
        val original = EvalReason("TARGETING_MATCH", "User ID", "target-123")
        val cached = EvalReason.withCachedSource(original)

        assertEquals("TARGETING_MATCH", cached.reason)
        assertEquals("User ID", cached.details)
        assertEquals("target-123", cached.targetId)
        assertEquals("CACHED", cached.source)
    }

}
