package com.dariopellegrini.vertirest.model

import com.dariopellegrini.vertirest.vertirest.models.GeoLocation
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class SamplePerson(
    var _id: String = "",
    var name: String? = null,
    var surname: String? = null)