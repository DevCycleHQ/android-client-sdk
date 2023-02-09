package com.devcycle.sdk.android.exception

import com.devcycle.sdk.android.model.ReadOnlyVariable
import com.devcycle.sdk.android.model.Variable

class DVCVariableException(message:String, currentVariable: Variable<Any>, updatedVariable: ReadOnlyVariable<Any>): Exception(message) {
    val currentVariable = currentVariable
    val updatedVariable = updatedVariable
}