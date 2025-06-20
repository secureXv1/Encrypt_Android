package com.safeguard.encrypt_android.data

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object TunnelService {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://symbolsaps.ddns.net:8000"

    fun crearTunel(nombre: String, password: String, callback: (Boolean, String?) -> Unit) {
        val json = JSONObject()
        json.put("nombre", nombre)
        json.put("password", password)

        val request = Request.Builder()
            .url("$BASE_URL/api/crear_tunel")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val id = JSONObject(it.body!!.string()).optInt("tunnel_id")
                        callback(true, id.toString())
                    } else {
                        callback(false, it.message)
                    }
                }
            }
        })
    }

    fun verificarTunel(nombre: String, password: String, callback: (Boolean, Int?, String?) -> Unit) {
        val json = JSONObject()
        json.put("nombre", nombre)
        json.put("password", password)

        val request = Request.Builder()
            .url("$BASE_URL/api/verificar_tunel")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val obj = JSONObject(it.body!!.string())
                        val id = obj.optInt("tunnel_id")
                        callback(true, id, null)
                    } else {
                        callback(false, null, "Credenciales incorrectas")
                    }
                }
            }
        })
    }
}
