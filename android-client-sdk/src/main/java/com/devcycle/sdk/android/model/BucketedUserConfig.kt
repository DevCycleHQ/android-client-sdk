package com.devcycle.sdk.android.model

import com.google.gson.annotations.SerializedName
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

/**
 * ClientSDKAPIResponse
 */
data class BucketedUserConfig internal constructor(
    @get:Schema(required = true, description = "")
    @SerializedName("project")
    val project: Project? = null,
    @get:Schema(required = true, description = "")
    @SerializedName("environment")
    val environment: Environment? = null,
    @get:Schema(description = "Mapping of `Feature.key` to `Feature` schema values.")
    @SerializedName("features")
    val features: Map<String, Feature>? = null,
    @get:Schema(description = "Map of `Feature._id` to `Feature._variation` used for event logging.")
    @SerializedName("featureVariationMap")
    val featureVariationMap: Map<String, String>? = null,
    @get:Schema(description = "Map of `Variable.key` to `Variable` values.")
    @SerializedName("variables")
    val variables: Map<String, Variable<Any>>? = null,
    @get:Schema(description = "Hashes `murmurhash.v3(variable.key + environment.apiKey)` of all known variable keys not contained in the `variables` object.")
    @SerializedName("knownVariableKeys")
    val knownVariableKeys: List<BigDecimal>? = null,
    @get:Schema(description = "Contains the SSE connection URL")
    @SerializedName("sse")
    val sse: SSE? = null
)