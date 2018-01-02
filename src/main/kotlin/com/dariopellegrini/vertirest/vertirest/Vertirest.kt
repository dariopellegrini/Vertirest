package com.dariopellegrini.vertirest.vertirest

import com.dariopellegrini.vertirest.vertirest.completion.RoutesCompletion
import com.dariopellegrini.vertirest.vertirest.permission.PermissionsDescriptor
import com.dariopellegrini.vertirest.vertirest.social.FacebookConfiguration
import com.dariopellegrini.vertirest.vertirest.user.UserConfigurator
import com.dariopellegrini.vertirest.vertirest.user.VertirestUser
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.core.spi.BufferFactory
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.io.File


class Vertirest(val vertx: Vertx, mongoConnection: String) {

    val router: Router = Router.router(vertx)
    private val mongo: MongoClient

    init {
        val mongoConfig = JsonObject()
                .put("connection_string", mongoConnection)
        mongo = MongoClient.createShared(vertx, mongoConfig)
    }

    fun startHttpServer(port: Int, host: String, listenHandler: Handler<AsyncResult<HttpServer>>? = null) {
        vertx.createHttpServer().requestHandler(router::accept).listen(port, host, listenHandler)
    }

    fun startHttpServer(port: Int, listenHandler: Handler<AsyncResult<HttpServer>>? = null) {
        vertx.createHttpServer().requestHandler(router::accept).listen(port, listenHandler)
    }

    fun startHttpServer(listenHandler: Handler<AsyncResult<HttpServer>>? = null) {
        vertx.createHttpServer().requestHandler(router::accept).listen(listenHandler)
    }

    fun startHttpServer() {
        vertx.createHttpServer().requestHandler(router::accept).listen()
    }

    fun <T> configureRoutes(collectionName: String, entityClass: Class<T>,
                            permissions: PermissionsDescriptor? = null,
                            indicies: Map<String, String>? = null,
                            routesCompletion: RoutesCompletion? = null) {
        val configurator = RoutesConfigurator(collectionName, mongo, entityClass, permissions, routesCompletion)
        configurator.configureRoutes(router)
        configurator.configureIndices()
    }

    fun <T: VertirestUser>configureUser(entityClass: Class<T>, facebookConfiguration: FacebookConfiguration? = null) {
        val configurator = UserConfigurator(mongo, entityClass, facebookConfiguration)
        configurator.configureUser(router)
    }
}