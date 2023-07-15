package com.devcycle.example

import androidx.constraintlayout.solver.state.Reference
import com.devcycle.sdk.android.api.DVCClient
import com.devcycle.sdk.android.exception.DVCException
import com.devcycle.sdk.android.model.DVCUser
import com.devcycle.sdk.android.model.Variable
import com.devcycle.sdk.android.util.DVCLogger
import com.devcycle.sdk.android.util.LogLevel
import org.json.JSONArray
import org.json.JSONObject

object DevCycleManager {
    var dvcClient: DVCClient? = null

    fun setClient(client: DVCClient) {
        dvcClient = client
    }

    @Throws
    fun variable(key: String, defaultValue: String): Variable<String> {
        if(dvcClient == null){
            throw DVCException("Missing client")
        }
        var variable = dvcClient!!.variable(key, defaultValue)
        if(variable.isDefaulted == true){
            println("Variable value "+  variable.value)
            // track variableDefaulted event
        } else {
            println("Variable value "+  variable.value)
            // track variableEvaluated event
        }
        return variable
    }

    fun variable(key: String, defaultValue: Number): Variable<Number> {
        if(dvcClient == null){
            throw DVCException("Missing client")
        }
        var variable = dvcClient!!.variable(key, defaultValue)
        if(variable.isDefaulted == true){
            // track variableDefaulted event
        } else {
            // track variableEvaluated event
        }
        return variable
    }

    fun variable(key: String, defaultValue: Boolean): Variable<Boolean> {
        if(dvcClient == null){
            throw DVCException("Missing client")
        }
        var variable = dvcClient!!.variable(key, defaultValue)
        if(variable.isDefaulted == true){
            // track variableDefaulted event
        } else {
            // track variableEvaluated event
        }
        return variable
    }

    fun variable(key: String, defaultValue: JSONObject): Variable<JSONObject> {
        if(dvcClient == null){
            throw DVCException("Missing client")
        }
        var variable = dvcClient!!.variable(key, defaultValue)
        if(variable.isDefaulted == true){
            // track variableDefaulted event
        } else {
            // track variableEvaluated event
        }
        return variable
    }

    fun variable(key: String, defaultValue: JSONArray): Variable<JSONArray> {
        if(dvcClient == null){
            throw DVCException("Missing client")
        }
        var variable = dvcClient!!.variable(key, defaultValue)
        if(variable.isDefaulted == true){
            // track variableDefaulted event
        } else {
            // track variableEvaluated event
        }
        return variable
    }
}

