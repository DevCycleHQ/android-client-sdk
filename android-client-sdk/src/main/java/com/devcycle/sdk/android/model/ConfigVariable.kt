package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import org.json.JSONArray
import org.json.JSONObject

class VariableDeserializer : JsonDeserializer<BaseConfigVariable>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BaseConfigVariable {
        val mapper = p.codec
        val root: ObjectNode = mapper.readTree<TreeNode>(p) as ObjectNode
        return if (root.get("value").nodeType == JsonNodeType.OBJECT) {
            mapper.treeToValue(root, JSONObjectConfigVariable::class.java)
        } else if (root.get("value").nodeType == JsonNodeType.ARRAY) {
            mapper.treeToValue(root, JSONArrayConfigVariable::class.java)
        } else if (root.get("value").nodeType == JsonNodeType.NUMBER) {
            mapper.treeToValue(root, NumberConfigVariable::class.java);
        } else if (root.get("value").nodeType == JsonNodeType.STRING) {
            mapper.treeToValue(root, StringConfigVariable::class.java);
        } else if (root.get("value").nodeType == JsonNodeType.BOOLEAN) {
            mapper.treeToValue(root, BooleanConfigVariable::class.java)
        } else {
            throw JsonParseException(
                p,
                "Unable to parse variable value for key ${root.get("key").asText()}"
            )
        }
    }
}

@JsonDeserialize(using = VariableDeserializer::class)
abstract class BaseConfigVariable {
    abstract val id: String
    abstract val value: Any
    abstract val key: String
    abstract val type: Variable.TypeEnum
    abstract val eval: EvalReason?
}

@JsonDeserialize(`as` = StringConfigVariable::class)
class StringConfigVariable(
    @JsonProperty("_id")
    override val id: String,
    override val value: String,
    override val key: String,
    override val type: Variable.TypeEnum,
    override val eval: EvalReason?
) : BaseConfigVariable()

@JsonDeserialize(`as` = BooleanConfigVariable::class)
class BooleanConfigVariable(
    @JsonProperty("_id")
    override val id: String,
    override val value: Boolean,
    override val key: String,
    override val type: Variable.TypeEnum,
    override val eval: EvalReason?
) : BaseConfigVariable()

@JsonDeserialize(`as` = NumberConfigVariable::class)
class NumberConfigVariable(
    @JsonProperty("_id")
    override val id: String,
    override val value: Number,
    override val key: String,
    override val type: Variable.TypeEnum,
    override val eval: EvalReason?
) : BaseConfigVariable()

@JsonDeserialize(`as` = JSONObjectConfigVariable::class)
class JSONObjectConfigVariable(
    @JsonProperty("_id")
    override val id: String,
    override val value: JSONObject,
    override val key: String,
    override val type: Variable.TypeEnum,
    override val eval: EvalReason?
) : BaseConfigVariable()

@JsonDeserialize(`as` = JSONArrayConfigVariable::class)
class JSONArrayConfigVariable(
    @JsonProperty("_id")
    override val id: String,
    override val value: JSONArray,
    override val key: String,
    override val type: Variable.TypeEnum,
    override val eval: EvalReason?
) : BaseConfigVariable()