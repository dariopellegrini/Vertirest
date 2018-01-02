package com.dariopellegrini.vertirest.vertirest.social

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class CheckResultContainer {
    lateinit var data: CheckResult
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CheckResult(var app_id: String = "", var type: String = "", var application: String = "", var is_valid: Boolean = true, var user_id: String = "")