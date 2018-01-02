package com.dariopellegrini.vertirest.vertirest.completion

import io.vertx.core.json.JsonObject

interface RoutesCompletion {
    fun onCreateCompleted(input: JsonObject, output: JsonObject)
    fun onGetOneCompleted(input: JsonObject, fields: JsonObject?, output: JsonObject)
    fun onGetAllCompleted(input: JsonObject, fields: JsonObject?, output: List<JsonObject>)
    fun onDeleteOneCompleted(query: JsonObject, output: JsonObject)
    fun onDeleteAllCompleted(query: JsonObject, output: JsonObject)
    fun onUpdateOneCompleted(query: JsonObject, json: JsonObject, output: JsonObject)
    fun onUpdateAllCompleted(query: JsonObject, json: JsonObject, output: JsonObject)
}