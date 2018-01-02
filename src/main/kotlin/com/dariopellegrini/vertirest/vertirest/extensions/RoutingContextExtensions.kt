package com.dariopellegrini.vertirest.vertirest.extensions

import com.dariopellegrini.vertirest.vertirest.constants.StringConstants
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

val RoutingContext.formJson: JsonObject
    get() {
        val formMap = this.request().formAttributes()
        val json = JsonObject()
        formMap.entries().forEach {
            entry ->
            json.put(entry.key, entry.value)
        }
        return json
    }

val RoutingContext.contentTypeHeader: String
    get() {
        return this.request().headers()[StringConstants.CONTENT_TYPE]
    }