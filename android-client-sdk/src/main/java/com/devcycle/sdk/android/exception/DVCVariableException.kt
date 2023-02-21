package com.devcycle.sdk.android.exception

import com.devcycle.sdk.android.model.BaseConfigVariable
import com.devcycle.sdk.android.model.Variable

class DVCVariableException(message:String, currentVariable: Variable<Any>, updatedVariable: BaseConfigVariable): Exception(message) {
    val currentVariable = currentVariable
    val updatedVariable = updatedVariable
}