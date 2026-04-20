package com.devcycle.sdk.android.model

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SSEMessageTests {

    /**
     * Tests for the old/legacy SSE message format where the MessageEvent data
     * contains a JSON object with a "data" field that is a JSON-encoded string.
     *
     * Example:
     * {
     *   "data": "{\"type\":\"refetchConfig\",\"lastModified\":1713100000000,\"etag\":\"abc123\"}"
     * }
     */
    @Nested
    inner class LegacyEnvelopeFormat {

        @Test
        fun `parses envelope format with data as JSON string`() {
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 1713100000000L)
                put("etag", "\"33a64df551425fcc55e4d42a148795d9f25f89d4\"")
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            assertEquals(1713100000000L, result.lastModified)
            assertEquals("\"33a64df551425fcc55e4d42a148795d9f25f89d4\"", result.etag)
        }

        @Test
        fun `parses envelope format with empty type`() {
            val innerPayload = JSONObject().apply {
                put("type", "")
                put("lastModified", 1713100000000L)
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            assertEquals("", result!!.type)
            assertEquals(1713100000000L, result.lastModified)
            assertNull(result.etag)
        }

        @Test
        fun `type field with refetchConfig is kept as string not parsed as number`() {
            // This is the exact scenario that caused the NumberFormatException:
            // "refetchConfig".toLong() would throw NumberFormatException
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 1713100000000L)
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            // Verify type is a string, not a number
            assertInstanceOf(String::class.java, result.type)
        }
    }

    /**
     * Tests for the new SSE event format where the newer LD EventSource library
     * returns the full event object including envelope fields (id, timestamp, name,
     * channel, action, serial) and the "data" field is a JSON-encoded string.
     *
     * This is the format that caused the original bug when the SDK tried to parse
     * the outer envelope's fields instead of the inner data payload.
     *
     * Example:
     * {
     *   "id": "evt-123",
     *   "timestamp": 1713100000000,
     *   "name": "message",
     *   "channel": "dvc_client_...",
     *   "action": 15,
     *   "serial": "abc",
     *   "data": "{\"type\":\"refetchConfig\",\"lastModified\":1713100000000,\"etag\":\"abc123\"}"
     * }
     */
    @Nested
    inner class NewFullEventEnvelopeFormat {

        @Test
        fun `parses full event envelope with data as JSON string`() {
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 1713100000000L)
                put("etag", "\"33a64df551425fcc55e4d42a148795d9f25f89d4\"")
            }
            val fullEvent = JSONObject().apply {
                put("id", "evt-abc123")
                put("timestamp", 1713100000000L)
                put("name", "message")
                put("channel", "dvc_client_sdkkey_config")
                put("action", 15)
                put("serial", "serial-001")
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(fullEvent.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            assertEquals(1713100000000L, result.lastModified)
            assertEquals("\"33a64df551425fcc55e4d42a148795d9f25f89d4\"", result.etag)
        }

        @Test
        fun `ignores envelope fields and only reads inner data`() {
            // The envelope has a "timestamp" field that should NOT be confused
            // with the inner "lastModified"
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 9999999999999L)
                put("etag", "inner-etag")
            }
            val fullEvent = JSONObject().apply {
                put("id", "evt-123")
                put("timestamp", 1111111111111L) // different from inner lastModified
                put("name", "message")
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(fullEvent.toString())

            assertNotNull(result)
            // Should use the inner lastModified, not the envelope timestamp
            assertEquals(9999999999999L, result!!.lastModified)
            assertEquals("inner-etag", result.etag)
        }

        @Test
        fun `handles full event envelope with no inner lastModified`() {
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
            }
            val fullEvent = JSONObject().apply {
                put("id", "evt-123")
                put("timestamp", 1713100000000L)
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(fullEvent.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            assertNull(result.lastModified)
            assertNull(result.etag)
        }
    }

    /**
     * Tests for the direct format where the message data IS the inner payload
     * (no envelope wrapping).
     */
    @Nested
    inner class DirectPayloadFormat {

        @Test
        fun `parses direct payload without envelope`() {
            val directPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 1713100000000L)
                put("etag", "etag-direct")
            }

            val result = SSEMessage.parse(directPayload.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            assertEquals(1713100000000L, result.lastModified)
            assertEquals("etag-direct", result.etag)
        }

        @Test
        fun `parses direct payload with only type`() {
            val directPayload = JSONObject().apply {
                put("type", "refetchConfig")
            }

            val result = SSEMessage.parse(directPayload.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            assertNull(result.lastModified)
            assertNull(result.etag)
        }
    }

    /**
     * Edge case and error handling tests.
     */
    @Nested
    inner class EdgeCases {

        @Test
        fun `returns null for blank input`() {
            assertNull(SSEMessage.parse(""))
            assertNull(SSEMessage.parse("   "))
        }

        @Test
        fun `returns null for invalid JSON`() {
            assertNull(SSEMessage.parse("not json at all"))
            assertNull(SSEMessage.parse("{broken"))
        }

        @Test
        fun `returns null when data field is invalid JSON string`() {
            val envelope = JSONObject().apply {
                put("data", "not-valid-json")
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNull(result)
        }

        @Test
        fun `returns null when data field is unexpected type`() {
            val envelope = JSONObject().apply {
                put("data", 12345)
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNull(result)
        }

        @Test
        fun `handles missing type field with default empty string`() {
            val innerPayload = JSONObject().apply {
                put("lastModified", 1713100000000L)
                put("etag", "etag-no-type")
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            assertEquals("", result!!.type)
            assertEquals(1713100000000L, result.lastModified)
            assertEquals("etag-no-type", result.etag)
        }

        @Test
        fun `handles lastModified of zero as null`() {
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 0)
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            // lastModified of 0 should be treated as not present
            assertNull(result!!.lastModified)
        }

        @Test
        fun `handles negative lastModified as null`() {
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", -1)
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            assertNull(result!!.lastModified)
        }

        @Test
        fun `handles empty JSON object`() {
            val result = SSEMessage.parse("{}")

            assertNotNull(result)
            assertEquals("", result!!.type)
            assertNull(result.lastModified)
            assertNull(result.etag)
        }

        @Test
        fun `handles explicit null etag value`() {
            val innerPayload = JSONObject().apply {
                put("type", "refetchConfig")
                put("lastModified", 1713100000000L)
                put("etag", JSONObject.NULL)
            }
            val envelope = JSONObject().apply {
                put("data", innerPayload.toString())
            }

            val result = SSEMessage.parse(envelope.toString())

            assertNotNull(result)
            assertEquals("refetchConfig", result!!.type)
            assertEquals(1713100000000L, result.lastModified)
            assertNull(result.etag)
        }
    }
}
