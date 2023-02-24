package com.devcycle.sdk.android.model

import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.exception.DVCVariableException
import com.devcycle.sdk.android.util.JSONMapper
import com.devcycle.sdk.android.util.DVCLogger
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.json.JSONArray
import org.json.JSONObject
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.IllegalArgumentException

private val logger: DVCLogger = DVCLogger.getInstance()

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Variable
 */
class Variable<T> internal constructor(
    /**
     * unique database id
     * @return _id
     */
    @JsonProperty("_id")
    var id: String? = null,
    /**
     * Unique key by Project, can be used in the SDK / API to reference by &#x27;key&#x27; rather than _id.
     * @return key
     */
    val key: String,
    /**
     * Variable type
     * @return type
     */
    val type: TypeEnum,

    /**
     * Variable value can be a string, number, boolean, or JSON
     * @return value
     */
    var value: T,

    @JsonIgnore
    val defaultValue: T,

    @JsonIgnore
    var isDefaulted: Boolean? = null
) : PropertyChangeListener {

    /**
     * Variable type
     */
    enum class TypeEnum(@get:JsonValue val value: String) {
        STRING("String"), BOOLEAN("Boolean"), NUMBER("Number"), JSON("JSON");
    }

    @JsonIgnore
    var evalReason: String? = null

    @JsonIgnore
    private var callback: DVCCallback<Variable<T>>? = null

    @JsonIgnore
    private val coroutineScope: CoroutineScope = MainScope()

    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    private fun updateVariable(variable: BaseConfigVariable) {
        if (getType(variable.value) != type) {
            throw DVCVariableException("Cannot update Variable with a different type", this as Variable<Any>, variable)
        }
        id = variable.id
        val executeCallBack = hasValueChanged(value, variable.value as T)

        value = variable.value as T

        isDefaulted = false
        evalReason = variable.evalReason

        if (executeCallBack) {
            val self = this
            coroutineScope.launch {
                callback?.onSuccess(self)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    private fun defaultVariable() {
        val executeCallBack = hasValueChanged(value, defaultValue)
        isDefaulted = true

        if (executeCallBack) {
            value = defaultValue
            val self = this
            coroutineScope.launch {
                callback?.onSuccess(self)
            }
        }
    }

    private fun hasValueChanged(oldValue: T, newValue: T): Boolean {
        if (newValue!!::class == JSONObject::class || newValue!!::class == JSONArray::class) {
            val new = JSONMapper.mapper.readTree(newValue.toString())
            val existing = JSONMapper.mapper.readTree(oldValue.toString())

            if (new != existing) {
                return true
            }
        } else if (newValue != oldValue) {
            return true
        }

        return false
    }

    companion object {
        @JvmSynthetic internal fun <T: Any> initializeFromVariable(key: String, defaultValue: T, readOnlyVariable: BaseConfigVariable?): Variable<T> {
            val type = getType(defaultValue)
            if (readOnlyVariable != null && type != null && getType(readOnlyVariable.value) === type) {
                @Suppress("UNCHECKED_CAST")
                val returnVariable = Variable(
                    id = readOnlyVariable.id,
                    key = key,
                    value = readOnlyVariable.value as T,
                    type = type,
                    defaultValue = defaultValue as T
                )
                returnVariable.evalReason = readOnlyVariable.evalReason
                returnVariable.isDefaulted = false
                return returnVariable
            } else {
                val returnVariable = Variable(
                    key = key,
                    value = defaultValue,
                    type = getAndValidateType(defaultValue),
                    defaultValue = defaultValue
                )
                returnVariable.isDefaulted = true
                if (readOnlyVariable != null) {
                    logger.e("Mismatched variable type for variable: $key, using default")
                }
                return returnVariable
            }
        }

        private fun <T: Any> getType(value: T, fromReadOnlyVariable: Boolean = false): TypeEnum? {
            val typeClass = value::class.java

            var typeEnum = when {
                // Kotlin types
                String::class.java.isAssignableFrom(typeClass) -> TypeEnum.STRING
                Number::class.java.isAssignableFrom(typeClass) -> TypeEnum.NUMBER
                Boolean::class.java.isAssignableFrom(typeClass) -> TypeEnum.BOOLEAN
                JSONObject::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                JSONArray::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON

                // Java types
                java.lang.String::class.java.isAssignableFrom(typeClass) -> TypeEnum.STRING
                java.lang.Number::class.java.isAssignableFrom(typeClass) -> TypeEnum.NUMBER
                java.lang.Boolean::class.java.isAssignableFrom(typeClass) -> TypeEnum.BOOLEAN
                org.json.JSONObject::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                org.json.JSONArray::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                else -> null
            }

            return typeEnum
        }

        @JvmSynthetic internal fun <T: Any> getAndValidateType(defaultValue: T): TypeEnum {
            val type = getType(defaultValue)
            if (type != null) {
                return type
            }
            throw IllegalArgumentException("${defaultValue::class.java} is not a valid type. Must be String / Number / Boolean or JSONObject")
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == BucketedUserConfigListener.BucketedUserConfigObserverConstants.propertyChangeConfigUpdated) {
            val config = evt.newValue as BucketedUserConfig
            val variable: BaseConfigVariable? = config.variables?.get(key)
            logger.v("Triggering property change handler for $key")
            if (variable != null) {
                try {
                    updateVariable(variable)
                } catch (e: DVCVariableException) {
                    logger.e("Mismatched variable type for variable: ${variable.key}, using default")
                }
            } else {
                try {
                    defaultVariable()
                } catch (e: DVCVariableException) {
                    logger.e("Unable to restore variable to default")
                }
            }
        }
    }

    /**
     * To be notified when Variable.value changes register a callback by calling this method. The
     * callback will replace any previously registered callback.
     *
     * [callback] returns the updated Variable inside callback.onSuccess(..)
     */
    @Deprecated("Use the plain callback signature instead.")
    fun onUpdate(callback: DVCCallback<Variable<T>>) {
        this.callback = callback
    }

    /**
     * To be notified when Variable.value changes register a callback by calling this method. The
     * callback will replace any previously registered callback.
     *
     * [callback] called with the updated variable
     */
    fun onUpdate(callback: (Variable<T>) -> Unit) {
        this.callback = object: DVCCallback<Variable<T>> {
            override fun onSuccess(result: Variable<T>) {
                callback(result)
            }

            override fun onError(t: Throwable) {
                // no-op
            }
        }
    }
}