package com.devcycle.sdk.android.util

import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JSONMapper {
    val mapper = jacksonObjectMapper().registerModule(JsonOrgModule())
}