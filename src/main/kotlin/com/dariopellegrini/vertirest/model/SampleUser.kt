package com.dariopellegrini.vertirest.model

import com.dariopellegrini.vertirest.vertirest.user.VertirestUser

class SampleUser(val name: String? = null): VertirestUser() {
    override val passwordValidator: Boolean?
        get() = password.length >= 8
}