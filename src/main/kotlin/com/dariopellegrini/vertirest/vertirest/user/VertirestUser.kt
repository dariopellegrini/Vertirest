package com.dariopellegrini.vertirest.vertirest.user

open class VertirestUser {
    val _id: String = ""

    val username: String = ""

    val password: String = ""

    var token: String? = null

    val facebookID: String? = null

    private val missingUsername: Boolean
        get() = username.isEmpty()

    private val missingPassword: Boolean
        get() = password.isEmpty()

    open val usernameValidator: Boolean? = null

    private val validUsername: Boolean
    get() {
        return if (usernameValidator != null) {
            usernameValidator!!
        } else {
            true
        }
    }

    open val passwordValidator: Boolean? = null
    private val validPassword: Boolean
        get() {
            return if (passwordValidator != null) {
                passwordValidator!!
            } else {
                true
            }
        }

    val canRegister: Boolean
    get() = !missingUsername && validUsername && !missingPassword && validPassword

    val canRegisterWithFacebook: Boolean
        get() = !missingUsername && validUsername
}