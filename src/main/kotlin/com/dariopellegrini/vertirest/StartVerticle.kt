package com.dariopellegrini.vertirest

import com.dariopellegrini.vertirest.model.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import com.dariopellegrini.vertirest.vertirest.Vertirest
import com.dariopellegrini.vertirest.vertirest.completion.RoutesCompletion
import com.dariopellegrini.vertirest.vertirest.social.FacebookConfiguration
import io.vertx.core.json.JsonObject

class StartVerticle : AbstractVerticle() {

    override fun start(fut: Future<Void>) {
        val vertirest = Vertirest(vertx, "mongodb://user:password@localhost:23333")

        vertirest.configureRoutes("people", SamplePerson::class.java)
        vertirest.configureRoutes("weapons", SampleWeapon::class.java, permissions = SamplePermissions(), routesCompletion = routeCompletion)
        vertirest.configureUser(SampleUser::class.java, FacebookConfiguration(appID = "<ID>", appSecret = "<token>"))

        val port = System.getenv("PORT") ?: "5050"
        vertirest.startHttpServer(port.toInt(), "0.0.0.0")
    }

    val routeCompletion: RoutesCompletion = object: RoutesCompletion {
        override fun onGetOneCompleted(input: JsonObject, fields: JsonObject?, output: JsonObject) {
            print("Get one completed")
        }

        override fun onGetAllCompleted(input: JsonObject, fields: JsonObject?, output: List<JsonObject>) {
            print("Get all completed")
        }

        override fun onCreateCompleted(input: JsonObject, output: JsonObject) {
            print("Create completed")
        }

        override fun onDeleteOneCompleted(query: JsonObject, output: JsonObject) {
            print("Delete one completed")
        }

        override fun onDeleteAllCompleted(query: JsonObject, output: JsonObject) {
            print("Delete all completed")
        }

        override fun onUpdateOneCompleted(query: JsonObject, json: JsonObject, output: JsonObject) {
            print("Update one completed")
        }

        override fun onUpdateAllCompleted(query: JsonObject, json: JsonObject, output: JsonObject) {
            print("Update all completed")
        }
    }
}