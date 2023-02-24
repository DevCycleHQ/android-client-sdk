package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * ClientSDKAPIResponse
 */
data class BucketedUserConfig internal constructor(
    @get:Schema(required = true, description = "")
    val project: Project? = null,
    @get:Schema(required = true, description = "")
    val environment: Environment? = null,
    @get:Schema(description = "Mapping of `Feature.key` to `Feature` schema values.")
    val features: Map<String, Feature>? = null,
    @get:Schema(description = "Map of `Feature._id` to `Feature._variation` used for event logging.")
    val featureVariationMap: Map<String, String>? = null,
    @get:Schema(description = "Map of `Variable.key` to `Variable` values.")
    val variables: Map<String, BaseConfigVariable>? = null,
    @get:Schema(description = "Hashes `murmurhash.v3(variable.key + environment.apiKey)` of all known variable keys not contained in the `variables` object.")
    val knownVariableKeys: List<BigDecimal>? = null,
    @get:Schema(description = "Contains the SSE connection URL")
    val sse: SSE? = null
)