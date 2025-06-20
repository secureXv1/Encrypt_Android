package com.safeguard.encrypt_android.data

import android.os.Build
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

object ClientService {
    private const val BASE_URL = "http://symbolsaps.ddns.net:8000"
    private val client = OkHttpClient()

    fun registrarCliente(uuid: String, callback: (Boolean, String?) -> Unit) {
        val hostname = Build.MODEL
        val sistema = "Android ${Build.VERSION.RELEASE}"

        val json = JSONObject().apply {
            put("uuid", uuid)
            put("hostname", hostname)
            put("sistema", sistema)
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/registrar_cliente")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, "Error ${response.code}: ${response.message}")
                }
            }
        })
    }

    fun generarUUID(): String {
        // ⚠️ En producción, guarda esto en SharedPreferences
        return UUID.randomUUID().toString()
    }
}
