package com.dariopellegrini.vertirest.vertirest.extensions

import com.dariopellegrini.vertirest.vertirest.models.GeoLocation
import io.vertx.core.json.JsonObject

val <T>Class<T>.geoIndexJson: JsonObject?
    get() {
        var jsonObject: JsonObject? = null
        Class.forName(this.name).declaredFields.forEach { field ->
            if (GeoLocation::class.java.isAssignableFrom(field.type)) {
                if (jsonObject == null) {
                    jsonObject = JsonObject()
                }
                jsonObject?.put(field.name, "2dsphere")

            }
        }
        return jsonObject
    }