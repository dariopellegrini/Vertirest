package com.dariopellegrini.vertirest.vertirest.user

import com.dariopellegrini.vertirest.vertirest.constants.CollectionsNames
import com.dariopellegrini.vertirest.vertirest.social.FacebookConfiguration
import com.dariopellegrini.vertirest.vertirest.social.FacebookManager
import com.dariopellegrini.vertirest.vertirest.utilities.HashUtils
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler


class UserConfigurator<T: VertirestUser>(private val mongo: MongoClient,
                          private val entityClass: Class<T>,
                          private val facebookConfiguration: FacebookConfiguration? = null,
                          private val collectionName: String) {

    fun configureUser(router: Router) {
        // Register
        router.route("/$collectionName").handler(BodyHandler.create())
        router.post("/$collectionName").handler(this::register)

        // Login
        router.route("/$collectionName/login").handler(BodyHandler.create())
        router.post("/$collectionName/login").handler(this::login)

        // Logout
        router.route("/$collectionName/logout").handler(BodyHandler.create())
        router.post("/$collectionName/logout").handler(this::logout)

        // Get
        router.get("/$collectionName").handler(this::userDetails)

        // Delete
        router.delete("/$collectionName").handler(this::userDelete)

        // Update
        router.put("/$collectionName").handler(this::userUpdate)

        if (facebookConfiguration != null) {
            val facebookManager = FacebookManager(facebookConfiguration, mongo, entityClass, collectionName)
            // Facebook login
            router.route("/$collectionName/facebook").handler(BodyHandler.create())
            router.post("/$collectionName/facebook").handler(facebookManager::access)
        }
    }

    private fun register(routingContext: RoutingContext) {
        val json = routingContext.bodyAsJson
        if (json != null) {
            try {
                val user = Json.decodeValue(routingContext.bodyAsString, entityClass) as VertirestUser
                if (!user.canRegister) {
                    routingContext.response()
                            .setStatusCode(400)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Cannot register. Provide valid username and password\"}")
                } else {
                    val email = user.username
                    findUser(JsonObject().put("username", email), completion = {
                        resJson ->
                        if (resJson == null) {
                            val password = HashUtils.sha512(user.password)
                            json.put("password", password)
                            mongo.insert(collectionName, json) { res ->
                                if (res.failed()) {
                                    routingContext.response()
                                            .setStatusCode(400)
                                            .putHeader("content-type", "application/json; charset=utf-8")
                                            .end(Json.encodePrettily(res.cause()))
                                } else {
                                    val id = res.result()
                                    routingContext.response()
                                            .setStatusCode(200)
                                            .putHeader("content-type", "application/json; charset=utf-8")
                                            .end("{\"_id\":\"$id\"}")
                                }
                            }
                        } else {
                            routingContext.response()
                                    .setStatusCode(400)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end("{\"error\":\"This username already exists\"}")
                        }
                    }, errorHandler = {
                        errorString ->
                        routingContext.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"error\":\"$errorString\"}")
                    })
                }
            } catch(e: DecodeException) {
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"JSON format error\"}")
            }
        } else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Missing body\"}")
        }
    }

    private fun login(routingContext: RoutingContext) {
        val json = routingContext.bodyAsJson
        if (json == null) {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Missing body\"}")
        } else if (json.loginError != null) {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"${json.loginError}\"}")
        } else {
            val password = HashUtils.sha512(json.getString("password"))
            json.put("password", password)
            findUser(json, completion = {
                resJson ->
                if (resJson != null) {
                    registerToken(resJson.getString("_id"),
                            completion = {
                                token ->
                                routingContext.response()
                                        .setStatusCode(200)
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .putHeader("token", token)
                                        .end(Json.encodePrettily(resJson))
                            },
                            errorHandler = {
                                errorString ->
                                routingContext.response()
                                        .setStatusCode(400)
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .end("{\"error\":\"$errorString\"}")

                            })
                } else {
                    routingContext.response()
                            .setStatusCode(403)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Wrong username or password\"}")
                }
            }, errorHandler = {
                errorString ->
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"$errorString\"}")
            })
        }
    }

    private fun logout(routingContext: RoutingContext) {
        val token = routingContext.request().headers()["token"]
        if (token != null && !token.isEmpty()) {
            val query = JsonObject().put("token", token)
            val json = JsonObject().put("\$unset", JsonObject().put("token", 1))
            mongo.updateCollection(collectionName,
                    query,
                    json
            ) { res ->
                if (res.failed()) {
                    routingContext.response()
                            .setStatusCode(400)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Error during logout\"}")
                } else {
                    val docModified = res.result().docModified
                    if (docModified > 0) {
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"status\":\"Logout successful.\"}")
                    } else {
                        routingContext.response()
                                .setStatusCode(403)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"error\":\"Logout unsuccessful. Non valid token\"}")
                    }
                }
            }
        } else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Not authorized.\"}")
        }
    }

    fun userDetails(routingContext: RoutingContext) {
        val token = routingContext.request().headers()["token"]
        if (token != null && !token.isEmpty()) {
            findUser(JsonObject().put("token", token), completion = {
                resJson ->
                if (resJson != null) {
                    routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(resJson))
                } else {
                    routingContext.response()
                            .setStatusCode(404)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Nothing found\"}")
                }
            }, errorHandler = {
                errorString ->
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"$errorString\"}")
            })
        } else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Not authorized.\"}")
        }
    }

    private fun userDelete(routingContext: RoutingContext) {
        val token = routingContext.request().headers()["token"]
        if (token != null && !token.isEmpty()) {
            mongo.removeDocument(collectionName, JsonObject().put("token", token)) {
                res ->
                if (res.succeeded()) {
                    // If removedCount > 0 ok else there is no authorization
                    if (res.result().removedCount > 0) {
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(res.result()))
                    } else {
                        routingContext.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"error\":\"Not authorized.\"}")
                    }
                } else {
                    routingContext.response()
                            .setStatusCode(500)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Server error. DB error.\"}")
                }
            }
        } else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Not authorized.\"}")
        }
    }

    fun userUpdate(routingContext: RoutingContext) {
        val token = routingContext.request().headers()["token"]
        val json = routingContext.bodyAsJson
        if (token != null && !token.isEmpty()) {
            if (json != null) {
                try {
                    // Check is the JSON is correct with the user model
                    Json.decodeValue(routingContext.bodyAsString, entityClass)
                    mongo.updateCollection(collectionName, JsonObject().put("token", token), JsonObject().put("\$set", json)) {
                        res ->
                        if (res.succeeded()) {
                            // If docMatched > 0 ok else there is no authorization
                            if (res.result().docMatched > 0) {
                                routingContext.response()
                                        .setStatusCode(200)
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .end(Json.encodePrettily(res.result()))
                            } else {
                                routingContext.response()
                                        .setStatusCode(400)
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .end("{\"error\":\"Not authorized.\"}")
                            }
                        } else {
                            routingContext.response()
                                    .setStatusCode(500)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end("{\"error\":\"Server error. DB error.\"}")
                        }
                    }
                } catch(e: Exception) {
                    routingContext.response()
                            .setStatusCode(400)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"JSON format error\"}")
                }
            } else {
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Missing body.\"}")
            }
        } else {
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Not authorized.\"}")
        }
    }

    // Tokens
    private fun registerToken(userID: String, completion: (String?) -> Unit, errorHandler: (String) -> Unit) {
        val token = (Math.random() * 1000).toString()
        val query = JsonObject().put("_id", userID)
        val json = JsonObject().put("token", token)
        mongo.updateCollection(collectionName,
                query,
                // The update syntax: {$set, the json object containing the fields to update}
                JsonObject().put("\$set", json)
        ) { res ->
            if (res.failed()) {
                errorHandler("Error during login")
            } else {
                completion(token)
            }
        }
    }

    private fun findUser(json: JsonObject, completion: (JsonObject?) -> Unit, errorHandler: (String) -> Unit) {
        mongo.findOne(collectionName, json, JsonObject("{\"facebookID\":0, \"password\":0, \"token\":0}"), { res ->
            if (res.succeeded()) {
                val resJson = res.result()
                completion(resJson)
            } else {
                errorHandler(res.cause().stackTrace.toString())
            }
        })
    }

    // Extension
    private val JsonObject.loginError: String?
    get() {
        return if (this.getString("username") == null && this.getString("password") == null) {
            "Missing username and password"
        } else if (this.getString("username") == null) {
            "Missing username"
        } else if (this.getString("password") == null) {
            "Missing password"
        } else {
            null
        }
    }
}