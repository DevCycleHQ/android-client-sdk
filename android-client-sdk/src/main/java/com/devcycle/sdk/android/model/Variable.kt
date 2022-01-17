/*
 * DevCycle Client SDK API
 * Documents the DevCycle Client SDK API which powers bucketing and descisions for DevCycle's client SDKs.
 *
 * OpenAPI spec version: 1-oas3
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package com.devcycle.sdk.android.model

import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.exception.DVCVariableException
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import org.json.JSONObject
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.IllegalArgumentException

/**
 * Variable
 */
class Variable<T> internal constructor() : PropertyChangeListener {

    /**
     * unique database id
     * @return _id
     */
    @get:Schema(required = true, description = "unique database id")
    @JsonProperty("_id")
    var id: String? = null

    /**
     * Unique key by Project, can be used in the SDK / API to reference by &#x27;key&#x27; rather than _id.
     * @return key
     */
    @get:Schema(
        required = true,
        description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id."
    )
    var key: String? = null

    /**
     * Variable type
     */
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

    /**
     * Variable type
     * @return type
     */
    @get:Schema(required = true, description = "Variable type")
    var type: TypeEnum? = null

    /**
     * Variable value can be a string, number, boolean, or JSON
     * @return value
     */
    @get:Schema(
        required = true,
        description = "Variable value can be a string, number, boolean, or JSON"
    )
    var value: T? = null

    @JsonIgnore
    var isDefaulted: Boolean? = null

    @JsonIgnore
    var evalReason: String? = null

    @JsonIgnore
    var defaultValue: T? = null

    @JsonIgnore
    private var callback: DVCCallback<Variable<T>>? = null

    @Throws(IllegalArgumentException::class)
    private fun updateVariable(variable: Variable<Any>) {
        var executeCallBack = false
        if (variable.type != type) {
            throw DVCVariableException("Cannot update Variable with a different type", this as Variable<Any>, variable)
        }
        id = variable.id
        if (variable.value != value) {
            executeCallBack = true
        }
        value = variable.value as T?
        isDefaulted = false
        evalReason = variable.evalReason

        if (executeCallBack) {
            callback?.onSuccess(this)
        }
    }

    companion object {
        @JvmSynthetic internal fun <T: Any> initializeFromVariable(key: String, defaultValue: T, variable: Variable<Any>?): Variable<T> {
            val returnVariable = Variable<T>();
            if (variable != null) {
                returnVariable.id = variable.id
                returnVariable.key = variable.key
                returnVariable.value = variable.value as T?
                returnVariable.type = variable.type
                returnVariable.evalReason = variable.evalReason
                returnVariable.isDefaulted = variable.isDefaulted
            } else {
                returnVariable.key = key
                returnVariable.value = defaultValue
                returnVariable.defaultValue = defaultValue
                returnVariable.isDefaulted = true
                returnVariable.type = getType(defaultValue)
            }
            return returnVariable
        }

        private fun <T: Any> getType(value: T): TypeEnum? {
            val typeClass = value::class.java

            val typeEnum = when {
                typeClass.isAssignableFrom(String::class.java) -> TypeEnum.STRING
                typeClass.isAssignableFrom(Number::class.java) -> TypeEnum.NUMBER
                typeClass.isAssignableFrom(Boolean::class.java) -> TypeEnum.BOOLEAN
                typeClass.isAssignableFrom(JSONObject::class.java) -> TypeEnum.JSON
                else -> null
            }

            return typeEnum
        }

        @JvmSynthetic internal fun <T: Any> validateType(defaultValue: T) {
            getType(defaultValue)
                ?: throw IllegalArgumentException("${defaultValue::class.java} is not a valid type. Must be String / Number / Boolean or JSONObject")
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == BucketedUserConfigListener.BucketedUserConfigObserverConstants.propertyChangeConfigUpdated) {
            val config = evt.newValue as BucketedUserConfig
            val variable: Variable<Any>? = config.variables?.get(key)
            if (variable != null) {
                updateVariable(variable)
            }
        }
    }

    /**
     * To be notified when Variable.value changes register a callback by calling this method. The
     * callback will replace any previously registered callback.
     *
     * [callback] returns the updated Variable inside callback.onSuccess(..)
     */
    fun onUpdate(callback: DVCCallback<Variable<T>>) {
        this.callback = callback
    }
}