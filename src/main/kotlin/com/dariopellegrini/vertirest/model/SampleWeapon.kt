package com.dariopellegrini.vertirest.model

import com.dariopellegrini.vertirest.vertirest.models.GeoLocation
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class SampleWeapon(
    var _id: String = "",
    var name: String? = null,
    var type: String? = null,
    var attack: Long? = null,
    var image: String? = null,
    var users: List<String>? = null)