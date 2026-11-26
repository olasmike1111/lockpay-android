package com.example.lockpay

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api {
    /** POST with the device token. Returns parsed JSON, or null on any failure. Call off the main thread. */
    fun post(c: Context, path: String): JSONObject? = try {
        val conn = URL(Config.server(c).trimEnd('/') + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Authorization", "Bearer ${Config.token(c)}")
        conn.doOutput = true
        conn.setFixedLengthStreamingMode(0)
        conn.outputStream.close()
        if (conn.responseCode in 200..299)
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        else null
    } catch (e: Exception) {
        null
    }
}
