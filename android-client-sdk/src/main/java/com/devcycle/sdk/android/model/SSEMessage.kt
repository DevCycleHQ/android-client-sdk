package com.devcycle.sdk.android.model

import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the parsed inner payload of an SSE message containing
 * config change metadata.
 *
 * The SSE event may arrive in two formats:
 *
 * 1. **Envelope format** (newer LD EventSource clients): The full event object
 *    contains fields like `id`, `timestamp`, `name`, `channel`, etc., with the
 *    `data` field being a JSON-encoded string containing the actual message payload.
 *
 * 2. **Direct format** (legacy): The `data` field directly contains the message
 *    payload as a JSON object (with `type`, `lastModified`, `etag`).
 *
 * The [parse] method handles both formats.
 */
data class SSEMessage(
    val type: String,
    val lastModified: Long?,
    val etag: String?
) {
    companion object {
        /**
         * Parses raw SSE message data (from [com.launchdarkly.eventsource.MessageEvent.data])
         * into an [SSEMessage].
         *
         * Handles both the envelope format (where `data` is a JSON string needing
         * secondary deserialization) and the direct format (where the top-level object
         * or its `data` field is already the message payload).
         *
         * @param rawData the raw string from the SSE MessageEvent
         * @return the parsed [SSEMessage], or null if the data cannot be parsed
         */
        fun parse(rawData: String): SSEMessage? {
            if (rawData.isBlank()) return null

            val data = try {
                JSONObject(rawData)
            } catch (e: JSONException) {
                return null
            }

            val innerData = extractInnerData(data) ?: return null

            val lastModified = if (innerData.has("lastModified")) {
                innerData.optLong("lastModified", 0L).takeIf { it > 0 }
            } else null

            val type = if (innerData.has("type")) {
                innerData.optString("type", "")
            } else ""

            val etag = if (innerData.has("etag") && !innerData.isNull("etag")) {
                innerData.getString("etag")
            } else null

            return SSEMessage(type = type, lastModified = lastModified, etag = etag)
        }

        /**
         * Extracts the inner message data from the SSE event.
         *
         * If the top-level object has a `data` field:
         * - If `data` is a String, parse it as JSON (envelope format)
         * - If `data` is a JSONObject, use it directly
         *
         * If there is no `data` field, the top-level object itself is the message.
         */
        internal fun extractInnerData(data: JSONObject): JSONObject? {
            if (!data.has("data")) {
                return data
            }

            return when (val dataField = data.get("data")) {
                is String -> {
                    try {
                        JSONObject(dataField)
                    } catch (e: JSONException) {
                        null
                    }
                }
                is JSONObject -> dataField
                else -> null
            }
        }
    }
}
