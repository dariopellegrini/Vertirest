package com.dariopellegrini.vertirest.vertirest.social

import com.dariopellegrini.vertirest.vertirest.user.VertirestUser
import com.dariopellegrini.vertirest.vertirest.utilities.NetworkUtilities
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext

class FacebookManager<T: VertirestUser>(private val facebookConfiguration: FacebookConfiguration?, private val mongo: MongoClient, val entityClass: Class<T>, val collectionName: String) {

    // Check promises
    fun access(routingContext: RoutingContext) {
        verify(routingContext) {
            facebookID ->
            findUser(facebookID, routingContext) {
                user, json ->
                if (user != null) {
                    registerToken(user._id, routingContext) {
                        token ->
                        user.token = token
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .putHeader("token", token)
                                .end(Json.encodePrettily(json))
                    }
                } else {
                    register(routingContext) {
                        findUser(facebookID, routingContext) {
                            user, json ->
                            if (user != null) {
                                registerToken(user._id, routingContext) {
                                    token ->
                                    user.token = token
                                    routingContext.response()
                                            .setStatusCode(200)
                                            .putHeader("content-type", "application/json; charset=utf-8")
                                            .putHeader("token", token)
                                            .end(Json.encodePrettily(json))
                                }
                            } else {
                                routingContext.response()
                                        .setStatusCode(500)
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .end("{\"error\":\"Registration error.\"}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun register(routingContext: RoutingContext, completion: () -> Unit) {
        val json = routingContext.bodyAsJson
        if (json != null) {
            try {
                val user = Json.decodeValue(routingContext.bodyAsString, entityClass) as VertirestUser
                if (user.canRegisterWithFacebook) {
                    mongo.insert(collectionName, json) { res ->
                        if (res.succeeded()) {
                            completion()
                        } else {
                            routingContext.response()
                                    .setStatusCode(400)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(Json.encodePrettily(res.cause()))
                        }
                    }
                } else {
                    routingContext.response()
                            .setStatusCode(400)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Cannot register. Provide valid username\"}")
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

    private fun findUser(facebookID: String, routingContext: RoutingContext, completion: (T?, jsonObject: JsonObject?) -> Unit) {
        val query = JsonObject().put("facebookID", facebookID)
        mongo.findOne(collectionName, query, JsonObject("{\"password\":0, \"token\":0}"), { res ->
            if (res.succeeded()) {
                if (res.result() != null && res.result().getString("_id") != null) {
                    try {
                        val user = Json.decodeValue(res.result().toString(), entityClass)
                        completion(user, res.result())
                    } catch(e: Exception) {
                        routingContext.response()
                                .setStatusCode(500)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"error\":\"Server error while reading user.\"}")
                    }
                } else {
                    completion(null, null)
                }
            } else {
                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Server error. DB error.\"}")
            }
        })
    }

    private fun verify(routingContext: RoutingContext, completion: (String) -> Unit) {
        if (facebookConfiguration != null) {
            val json = routingContext.bodyAsJson
            if (json == null) {
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Missing body\"}")
            }

            val token = json.getString("token")
            if (token == null) {
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Missing Facebook token.\"}")
            }

            val facebookID = json.getString("facebookID")
            if (facebookID == null) {
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Missing Facebook user ID.\"}")
            }

            // Check if the token is correct
            /*
            https://graph.facebook.com/debug_token?input_token=EAALk5zZCHEFABAEx8TMfbu40HHKKoTJxb24ZBk8OV4gzRK54dMMgKVwToLj4kFZC8NkbJY0gjd9s8ld81qxZAlmPZBlDj6BVH6qvjqZBCUMVu7cjYZAJ1mTS1cHZCkdZACwitOXFAvien8CylOgBh0sKskTxVfiIWCCqGRcZBOWCfuiKmut2FxbN7LZAeBCZBK7DGfQmkGziFAmNf3A5C29iD7WT5PMlik37ZArEZD&access_token=814631812010064|c46cd55ff6312099704b985574b6161b
            10210216807007322
            */
//        val endpoint = "https://graph.facebook.com/me?access_token=${token}"
            val endpoint = "https://graph.facebook.com/debug_token?input_token=$token&access_token=${facebookConfiguration.appID}|${facebookConfiguration.appSecret}"

            NetworkUtilities.executeGet(endpoint) {
                _, result, error ->
                if (result != null) {
                    try {
                        val resultObject = Json.decodeValue(result, CheckResultContainer::class.java).data
                        if (resultObject.is_valid){ // Is not mapped
                            if (resultObject.user_id == facebookID) {
                                completion(facebookID)
                            } else {
                                routingContext.response()
                                        .setStatusCode(400)
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .end("{\"error\":\"Facebook user ID is not valid\"}")
                            }
                        } else {
                            routingContext.response()
                                    .setStatusCode(400)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end("{\"error\":\"Facebook token is not valid\"}")
                        }
                    } catch (e: Exception) {
                        routingContext.response()
                                .setStatusCode(500)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end("{\"error\":\"Server error. Non valid response from Facebook.\"}")
                    }
                } else {
                    routingContext.response()
                            .setStatusCode(400)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end("{\"error\":\"Facebook error. Token or user ID not verified.\"}")
                }

            }
        } else {
            routingContext.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{\"error\":\"Facebook not configured on server\"}")
        }
    }

    private fun registerToken(userID: String, routingContext: RoutingContext, completion: (String?) -> Unit) {
        val token = (Math.random() * 1000).toString()
        val query = JsonObject().put("_id", userID)
        val json = JsonObject().put("token", token)
        mongo.updateCollection(collectionName,
                query,
                // The update syntax: {$set, the json object containing the fields to update}
                JsonObject().put("\$set", json)
        ) { res ->
            if (res.succeeded()) {
                completion(token)
            } else {
                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end("{\"error\":\"Server error. DB error.\"}")
            }
        }
    }
}