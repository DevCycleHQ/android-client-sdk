package com.devcycle.android.client.sdk.exception

import com.devcycle.android.client.sdk.model.Variable

class DVCVariableException(message:String, currentVariable: Variable<Any>, updatedVariable: Variable<Any>): Exception(message) {
    val currentVariable = currentVariable
    val updatedVariable = updatedVariable
}