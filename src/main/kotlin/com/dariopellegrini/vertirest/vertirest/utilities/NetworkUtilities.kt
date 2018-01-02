package com.dariopellegrini.vertirest.vertirest.utilities

import java.io.InputStreamReader
import java.io.BufferedReader
import io.vertx.core.json.JsonObject
import java.net.HttpURLConnection
import java.net.URL


class NetworkUtilities {
    companion object {
        fun executeGet(targetURL: String, completion: (statusCode: Int, String?, String?) -> Unit) {
            Thread {
                val result = StringBuilder()
                val url = URL(targetURL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val rd = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String? = rd.readLine()
                while (line != null) {
                    result.append(line)
                    line = rd.readLine()
                }
                rd.close()
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    completion(connection.responseCode, result.toString(), null)
                } else {
                    completion(connection.responseCode, null, result.toString())
                }
            }.start()
        }
    }
}