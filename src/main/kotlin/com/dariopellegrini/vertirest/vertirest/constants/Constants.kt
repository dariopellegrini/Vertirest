package com.dariopellegrini.vertirest.vertirest.constants

enum class RESTPermission {
    READ, READ_PUBLIC, CREATE, UPDATE, DELETE
}

object CollectionsNames {
    val USER_COLLECTION = "users"
}

object StringConstants {
    val CONTENT_TYPE = "Content-Type"
    val MULTIPART_FORM_DATA = "multipart/form-data"
    val FILE_UPLOADS = "file-uploads"
}