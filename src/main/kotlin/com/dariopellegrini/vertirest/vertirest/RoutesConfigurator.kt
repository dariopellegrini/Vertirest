package com.dariopellegrini.vertirest.vertirest

import com.dariopellegrini.vertirest.vertirest.completion.RoutesCompletion
import com.dariopellegrini.vertirest.vertirest.constants.RESTPermission
import com.dariopellegrini.vertirest.vertirest.constants.StringConstants
import com.dariopellegrini.vertirest.vertirest.extensions.contentTypeHeader
import com.dariopellegrini.vertirest.vertirest.extensions.formJson
import com.dariopellegrini.vertirest.vertirest.extensions.geoIndexJson
import com.dariopellegrini.vertirest.vertirest.files.FileManager
import com.dariopellegrini.vertirest.vertirest.permission.PermissionChecker
import com.dariopellegrini.vertirest.vertirest.permission.PermissionsDescriptor
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.FindOptions
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.io.File
import java.util.stream.Collectors

class RoutesConfigurator<T>(private val collection: String,
                            private val mongo: MongoClient,
                            private val entityClass: Class<T>,
                            private val permissionsDescriptor: PermissionsDescriptor? = null,
                            private val routesCompletion: RoutesCompletion? = null) {

    private val permissionsChecker = PermissionChecker(collection, mongo, permissionsDescriptor)
    fun configureRoutes(router: Router) {
        // Create
        router.route("/$collection*").handler(BodyHandler.create())
        router.post("/$collection").handler(this::createOneSecure)

        // Get
        router.get("/$collection").handler(this::getAllSecure)
        router.get("/$collection/:id").handler(this::getOneSecure)

        // Delete
        router.delete("/$collection").handler(this::deleteAllSecure)
        router.delete("/$collection/:id").handler(this::deleteOneSecure)

        // Update
        router.put("/$collection/:id").handler(this::updateOneSecure)
        router.put("/$collection").handler(this::updateAllSecure)

        // Files
        router.get("/$collection/uploads/:name").handler(this::getFile)
    }

    fun configureIndices(indices: Map<String, String>? = null) {
        entityClass.geoIndexJson?.let {
            json ->
            mongo.createIndex(collection, json) {
                res ->
                print(res)
            }
        }

        indices?.let {
            mongo.createIndex(collection, JsonObject(indices)) {
                res ->
                print(res)
            }
        }
    }

    private fun createOneSecure(routingContext: RoutingContext) {
        permissionsChecker.checkCreateOne(routingContext.request().headers()["token"]) {
            success, json ->
            if (success) {
                createOne(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }

    private fun createOne(routingContext: RoutingContext, permissionJson: JsonObject? = null) {
        try {
            val json = if (routingContext.contentTypeHeader.contains(StringConstants.MULTIPART_FORM_DATA)) {
                routingContext.formJson
            } else {
                routingContext.bodyAsJson
            }
            val upload = routingContext.fileUploads()
            if (upload.size > 0) {
                upload.forEach {
                    file ->
                    FileManager.retrieveFile(file)?.let {
                        pair ->
                        json.put(pair.first, pair.second)
                    }
                }
            }

            Json.decodeValue(json.toString(), entityClass)
            permissionJson?.let {
                json.mergeIn(permissionJson)
            }
            mongo.insert(collection, json) { res ->
                if (res.failed()) {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(res.cause()))
                } else {
                    json.put("_id", res.result())
                    routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(json))
                    routesCompletion?.onCreateCompleted(json, json)
                }
            }
        } catch(e: DecodeException) {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"JSON format error\"}")
        }
    }

    private fun getOneSecure(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        permissionsChecker.checkOne(routingContext.request().headers()["token"], id, RESTPermission.READ) {
            success, json ->
            if (success) {
                getOne(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }
    
    private fun getOne(routingContext: RoutingContext, permissionJson: JsonObject?) {
        val query = JsonObject()
        val id = routingContext.request().getParam("id")
        query.put("_id", id)
        permissionJson?.let {
            query.mergeIn(permissionJson)
        }

        var fields: JsonObject? = null
        if (query.containsKey("\$select")) {
            fields = JsonObject(query.getString("\$select"))
            query.remove("\$select")
        }

        mongo.findOne(collection, query, fields, { res ->
            if (res.succeeded()) {
                val json = res.result()
                if (json == null) {
                    routingContext.response()
                            .setStatusCode(404)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Nothing found\"}")
                } else {
                    try {
                        val entity = Json.decodeValue(res.result().toString(), entityClass)
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(entity))
                        routesCompletion?.onGetOneCompleted(query, fields, json)
                    } catch(e: DecodeException) {
                        routingContext.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"error\":\"Error during mapping\"}")
                    }
                }
            } else {
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"${res.cause().printStackTrace()}\"}")
            }
        })
    }

    private fun getAllSecure(routingContext: RoutingContext) {
        permissionsChecker.checkAll(routingContext.request().headers()["token"], RESTPermission.READ) {
            success, json ->
            if (success) {
                getAll(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }
    
    private fun getAll(routingContext: RoutingContext, permissionJson: JsonObject? = null) {
        val query = JsonObject()

        // Add GET parameters to query
        routingContext.request().params().forEach {
            param ->
            query.put(param.key, param.value)
        }

        // $query param can contains JSON for a MongoDB query. Should be added to query
        if (query.containsKey("\$query")) {
            val mongoQuery = JsonObject(query.getString("\$query"))
            query.remove("\$query")
            query.mergeIn(mongoQuery)
        }

        // $select param can contains JSON for a MongoDB field selection.
        var fields: JsonObject? = null
        if (query.containsKey("\$select")) {
            try {
                fields = JsonObject(query.getString("\$select"))
            } catch (e: DecodeException) {
            }
            query.remove("\$select")
        }

        // $sort param can contains JSON for a MongoDB field selection.
        var sort: JsonObject? = null
        if (query.containsKey("\$sort")) {
            try {
                sort = JsonObject(query.getString("\$sort"))
            } catch (e: DecodeException) {
                e.printStackTrace()
            }
            query.remove("\$sort")
        }

        // $limit option
        var limit: Int? = null
        if (query.containsKey("\$limit")) {
            try {
                limit = query.getString("\$limit").toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            query.remove("\$limit")
        }

        // $skip option
        var skip: Int? = null
        if (query.containsKey("\$skip")) {
            try {
                skip = query.getString("\$skip").toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            query.remove("\$skip")
        }

        val findOptions = FindOptions()
        fields?.let {
            findOptions.fields = fields
        }
        sort?.let {
            findOptions.sort = sort
        }
        limit?.let {
            findOptions.limit = limit
        }
        skip?.let {
            findOptions.skip = skip
        }

        // Add permission json
        permissionJson?.let {
            query.mergeIn(permissionJson)
        }

        mongo.findWithOptions(collection, query, findOptions, { res ->
            if (res.succeeded()) {
                val objects = res.result()
                val entities = objects.stream().map { mapper ->
                    try {
                        Json.decodeValue(mapper.encodePrettily(), entityClass)
                    } catch(e: DecodeException) {
                    }
                }.collect(Collectors.toList())
                routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(entities))
                routesCompletion?.onGetAllCompleted(query, fields, res.result())
            } else {
                routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(res.cause()))
            }
        })
    }

    private fun deleteOneSecure(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        permissionsChecker.checkOne(routingContext.request().headers()["token"], id, RESTPermission.DELETE) {
            success, json ->
            if (success) {
                deleteOne(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }
    
    private fun deleteOne(routingContext: RoutingContext, permissionJson: JsonObject?) {
        val id = routingContext.request().getParam("id")
        if (id == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"No id provided\"}")
        } else {
            val query = JsonObject().put("_id", id)
            permissionJson?.let {
                query.mergeIn(permissionJson)
            }
            mongo.removeDocument(collection, query
            ) { res ->
                if (res.failed()) {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(res.cause()))
                } else {
                    routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(res.result()))
                    routesCompletion?.onDeleteOneCompleted(query, JsonObject(Json.encodePrettily(res.result())))
                } }
        }
    }

    private fun deleteAllSecure(routingContext: RoutingContext) {
        permissionsChecker.checkAll(routingContext.request().headers()["token"], RESTPermission.DELETE) {
            success, json ->
            if (success) {
                deleteAll(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }
    
    private fun deleteAll(routingContext: RoutingContext, permissionJson: JsonObject?) {
        val query = JsonObject(routingContext.bodyAsString)
        permissionJson?.let {
            query.mergeIn(permissionJson)
        }
        mongo.removeDocuments(collection, query
        ) { res ->
            if (res.failed()) {
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(res.cause()))
            } else {
                routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(res.result()))
                routesCompletion?.onDeleteAllCompleted(query, JsonObject(Json.encodePrettily(res.result())))
            } }
    }

    private fun updateOneSecure(routingContext: RoutingContext) {
        val id = routingContext.request().getParam("id")
        permissionsChecker.checkOne(routingContext.request().headers()["token"], id, RESTPermission.UPDATE) {
            success, json ->
            if (success) {
                updateOne(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }
    
    private fun updateOne(routingContext: RoutingContext, permissionJson: JsonObject?) {
        val id = routingContext.request().getParam("id")
        val json = if (routingContext.contentTypeHeader.contains(StringConstants.MULTIPART_FORM_DATA)) {
            routingContext.formJson
        } else {
            routingContext.bodyAsJson
        }
        val upload = routingContext.fileUploads()
        if (upload.size > 0) {
            upload.forEach {
                file ->
                FileManager.retrieveFile(file)?.let {
                    pair ->
                    json.put(pair.first, pair.second)
                }
            }
        }
        val query = JsonObject().put("_id", id)
        permissionJson?.let {
            query.mergeIn(permissionJson)
        }
        try {
            Json.decodeValue(routingContext.bodyAsString, entityClass)
            if (id == null || json == null) {
                routingContext.response().setStatusCode(400).end()
            } else {
                mongo.updateCollection(collection,
                        query, // Select a unique document
                        // The update syntax: {$set, the json object containing the fields to update}
                        JsonObject()
                                .put("\$set", json)
                ) { res ->
                    if (res.failed()) {
                        routingContext.response().setStatusCode(404).end("{\"error\":\"Document not found\"}")
                    } else {
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(res.result()))
                        routesCompletion?.onUpdateOneCompleted(query, json, JsonObject(Json.encodePrettily(res.result())))
                    }
                }
            }
        } catch(e: DecodeException) {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"JSON format error\"}")
        }
    }

    private fun updateAllSecure(routingContext: RoutingContext) {
        permissionsChecker.checkAll(routingContext.request().headers()["token"], RESTPermission.UPDATE) {
            success, json ->
            if (success) {
                updateAll(routingContext, json)
            } else {
                routingContext.response()
                        .setStatusCode(403)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Not authorized\"}")
            }
        }
    }
    
    private fun updateAll(routingContext: RoutingContext, permissionJson: JsonObject?) {
        val query = JsonObject()

        routingContext.request().params().forEach {
            param ->
            query.put(param.key, param.value)
        }

        if (query.containsKey("\$query")) {
            val mongoQuery = JsonObject(query.getString("\$query"))
            query.remove("\$query")
            query.mergeIn(mongoQuery)
        }

        permissionJson?.let {
            query.mergeIn(permissionJson)
        }

        val json = if (routingContext.contentTypeHeader.contains(StringConstants.MULTIPART_FORM_DATA)) {
            routingContext.formJson
        } else {
            routingContext.bodyAsJson
        }
        val upload = routingContext.fileUploads()
        if (upload.size > 0) {
            upload.forEach {
                file ->
                FileManager.retrieveFile(file)?.let {
                    pair ->
                    json.put(pair.first, pair.second)
                }
            }
        }
        try {
            Json.decodeValue(routingContext.bodyAsString, entityClass)
            mongo.updateCollectionWithOptions(collection,
                    query,
                    // The update syntax: {$set, the json object containing the fields to update}
                    JsonObject().put("\$set", json),
                    UpdateOptions(false, true) // Upsert: set to true to insert the document if the query doesn’t match
            ) { res ->
                if (res.failed()) {
                    routingContext.response().setStatusCode(404).end("{\"error\":\"Document not found\"}")
                } else {
                    routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(res.result()))
                    routesCompletion?.onUpdateOneCompleted(query, json, JsonObject(Json.encodePrettily(res.result())))
                }
            }
        } catch(e: DecodeException) {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"JSON format error\"}")
        }
    }

    // Files
    private fun getFile(routingContext: RoutingContext) {
        val name = routingContext.request().getParam("name")

        if (name != null) {
            val file = File("${StringConstants.FILE_UPLOADS}/$name")
            if (file.exists()) {
                val buffer = Buffer.buffer().appendBytes(file.readBytes())
                routingContext.response().end(buffer)
            } else {
                routingContext.response()
                        .setStatusCode(404)
                        .end("Nothing found")
            }
        } else {
            routingContext.response()
                    .setStatusCode(400)
                    .end("Request error")
        }
    }
}