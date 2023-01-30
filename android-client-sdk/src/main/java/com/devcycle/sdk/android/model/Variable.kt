package com.devcycle.sdk.android.model

import com.devcycle.sdk.android.api.DVCCallback
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.exception.DVCVariableException
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.annotations.SerializedName
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.json.JSONObject
import timber.log.Timber
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.IllegalArgumentException

/**
 * Variable
 */
class Variable<T> internal constructor(
    /**
     * unique database id
     * @return _id
     */
    @get:Schema(required = true, description = "unique database id")
    @SerializedName("_id")
    var id: String? = null,
    /**
     * Unique key by Project, can be used in the SDK / API to reference by &#x27;key&#x27; rather than _id.
     * @return key
     */
    @get:Schema(
        required = true,
        description = "Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id."
    )
    @SerializedName("key")
    val key: String,
    /**
     * Variable type
     * @return type
     */
    @get:Schema(required = true, description = "Variable type")
    @SerializedName("type")
    val type: TypeEnum,

    /**
     * Variable value can be a string, number, boolean, or JSON
     * @return value
     */
    @get:Schema(
        required = true,
        description = "Variable value can be a string, number, boolean, or JSON"
    )
    @SerializedName("value")
    var value: T,

    @JsonIgnore
    @SerializedName("isDefaulted")
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
    var defaultValue: T? = null

    @JsonIgnore
    private var callback: DVCCallback<Variable<T>>? = null

    @JsonIgnore
    private val coroutineScope: CoroutineScope = MainScope()

    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    private fun updateVariable(variable: Variable<Any>) {
        var executeCallBack = false
        if (variable.type != type) {
            throw DVCVariableException("Cannot update Variable with a different type", this as Variable<Any>, variable)
        }
        id = variable.id
        if (variable.value != value) {
            executeCallBack = true
        }

        value = if (type == TypeEnum.JSON) {
            JSONObject(variable.value as MutableMap<Any?, Any?>) as T
        } else {
            variable.value as T
        }

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
        var executeCallBack = false
        if (value != defaultValue) {
            executeCallBack = true
            value = defaultValue!!
            isDefaulted = true
        }

        if (executeCallBack) {
            val self = this
            coroutineScope.launch {
                callback?.onSuccess(self)
            }
        }
    }

    companion object {
        @JvmSynthetic internal fun <T: Any> initializeFromVariable(key: String, defaultValue: T, variable: Variable<Any>?): Variable<T> {
            if (variable != null && variable.type === getType(defaultValue)) {
                @Suppress("UNCHECKED_CAST")
                val returnVariable = Variable(
                    id = variable.id,
                    key = variable.key,
                    value = variable.value as T,
                    type = variable.type
                )
                returnVariable.evalReason = variable.evalReason
                returnVariable.isDefaulted = variable.isDefaulted
                return returnVariable
            } else {
                val returnVariable = Variable(
                    key = key,
                    value = defaultValue,
                    type = getAndValidateType(defaultValue)
                )
                returnVariable.defaultValue = defaultValue
                returnVariable.isDefaulted = true
                if (variable != null) {
                    Timber.e("Mismatched variable type for variable: $key, using default")
                }
                return returnVariable
            }
        }

        private fun <T: Any> getType(value: T): TypeEnum? {
            val typeClass = value::class.java

            var typeEnum = when {
                // Kotlin types
                String::class.java.isAssignableFrom(typeClass) -> TypeEnum.STRING
                Number::class.java.isAssignableFrom(typeClass) -> TypeEnum.NUMBER
                Boolean::class.java.isAssignableFrom(typeClass) -> TypeEnum.BOOLEAN
                JSONObject::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                // Java types
                java.lang.String::class.java.isAssignableFrom(typeClass) -> TypeEnum.STRING
                java.lang.Number::class.java.isAssignableFrom(typeClass) -> TypeEnum.NUMBER
                java.lang.Boolean::class.java.isAssignableFrom(typeClass) -> TypeEnum.BOOLEAN
                org.json.JSONObject::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
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
            val variable: Variable<Any>? = config.variables?.get(key)
            if (variable != null) {
                try {
                    updateVariable(variable)
                } catch (e: DVCVariableException) {
                    Timber.e("Mismatched variable type for variable: ${variable.key}, using default")
                }
            } else {
                try {
                    defaultVariable()
                } catch (e: DVCVariableException) {
                    Timber.e("Unable to restore variable to default")
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