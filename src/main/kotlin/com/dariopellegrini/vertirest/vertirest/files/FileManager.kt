package com.dariopellegrini.vertirest.vertirest.files

import com.dariopellegrini.vertirest.vertirest.constants.StringConstants
import io.vertx.ext.web.FileUpload
import java.io.File

class FileManager {
    companion object {
        fun retrieveFile(file: FileUpload): Pair<String, String>? {
            val generatedName = file.uploadedFileName()
            val fileName = file.fileName().replace(" ", "")
            val label = file.name()
            val finalFileName = "${generatedName}-$fileName"
            File(generatedName).renameTo(File(finalFileName))
            return Pair(label, finalFileName.replace(StringConstants.FILE_UPLOADS, "").replace("/", ""))
        }
    }
}