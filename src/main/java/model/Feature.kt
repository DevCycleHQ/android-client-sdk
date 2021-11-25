package model

import lombok.AllArgsConstructor
import lombok.NoArgsConstructor
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data

@Data
@AllArgsConstructor
@NoArgsConstructor
class Feature {
    @Schema(required = true, description = "unique database id")
    @JsonProperty("_id")
    private val id: String? = null

    @Schema(
        required = true,
        description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id."
    )
    private val key: String? = null

    @Schema(required = true, description = "Feature type")
    private val type: TypeEnum? = null

    @Schema(required = true, description = "Bucketed feature variation")
    @JsonProperty("_variation")
    private val variation: String? = null

    @Schema(description = "Evaluation reasoning")
    private val evalReason: String? = null

    enum class TypeEnum(@get:JsonValue val value: String) {
        RELEASE("release"), EXPERIMENT("experiment"), PERMISSION("permission"), OPS("ops");

        override fun toString(): String {
            return value
        }

        companion object {
            fun fromValue(text: String): TypeEnum? {
                for (b in values()) {
                    if (b.value == text) {
                        return b
                    }
                }
                return null
            }
        }
    }
}