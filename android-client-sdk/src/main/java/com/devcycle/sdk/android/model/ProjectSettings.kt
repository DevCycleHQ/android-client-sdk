package com.devcycle.sdk.android.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)

class ProjectSettings {
    /**
     * edgeDB Project Settings
     * @return edgeDB
     */
    @get:Schema(required = false, description = "edgeDB Project Settings")
    @JsonProperty("edgeDB")
    var edgeDB: EdgeDB? = null

    fun edgeDB(edgeDB: EdgeDB): ProjectSettings {
        this.edgeDB = edgeDB
        return this
    }
}