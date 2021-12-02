package model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data

@Data
class Variable {
    @Schema(required = true, description = "unique database id")
    @JsonProperty("_id")
    private var id: String? = null

    @Schema(
        required = true,
        description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id."
    )
    private var key: String? = null

    @Schema(
        required = true,
        description = "Variable value can be a string, number, boolean, or JSON"
    )
    private var value: Any? = null

    @Schema(required = true, description = "Variable type")
    private var type: TypeEnum? = null

    enum class TypeEnum(@get:JsonValue val value: String) {
        STRING("String"), BOOLEAN("Boolean"), NUMBER("Number"), JSON("JSON");

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

    constructor(id: String?, key: String?, value: Any?, type: TypeEnum?) {
        this.id = id
        this.key = key
        this.value = value
        this.type = type
    }

    constructor() {}

    class VariableBuilder internal constructor() {
        private var id: String? = null
        private var key: String? = null
        private var value: Any? = null
        private var type: TypeEnum? = null
        @JsonProperty("_id")
        fun id(id: String?): VariableBuilder {
            this.id = id
            return this
        }

        fun key(key: String?): VariableBuilder {
            this.key = key
            return this
        }

        fun value(value: Any?): VariableBuilder {
            this.value = value
            return this
        }

        fun type(type: TypeEnum?): VariableBuilder {
            this.type = type
            return this
        }

        fun build(): Variable {
            return Variable(id, key, value, type)
        }

        override fun toString(): String {
            return "Variable.VariableBuilder(id=$id, key=$key, value=$value, type=$type)"
        }
    }

    companion object {
        fun builder(): VariableBuilder {
            return VariableBuilder()
        }
    }
}