package com.dariopellegrini.vertirest.model

import com.dariopellegrini.vertirest.vertirest.models.GeoLocation
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class SamplePointOfInterest(
    var _id: String = "",
    var name: String? = null,
    var owner: String? = null,
    var location: GeoLocation? = null,
    var items: List<String>? = null,
    var enemies: Map<String, Int>? = null,
    var people: List<String>? = null)