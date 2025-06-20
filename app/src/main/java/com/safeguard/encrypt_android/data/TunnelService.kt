package com.safeguard.encrypt_android.data

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import okhttp3.HttpUrl.Companion.toHttpUrl


object TunnelService {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://symbolsaps.ddns.net:8000"

    fun crearTunel(
        nombre: String,
        password: String,
        uuid: String,
        callback: (success: Boolean, tunnelId: Int?, errorMsg: String?) -> Unit
    ) {
        val json = JSONObject()
        json.put("name", nombre)
        json.put("password", password)
        json.put("uuid", uuid)

        val request = Request.Builder()
            .url("$BASE_URL/api/tunnels/create")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null, "Error de red: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyStr = it.body?.string()
                    if (it.isSuccessful) {
                        val obj = JSONObject(bodyStr ?: "{}")
                        val id = obj.optInt("tunnel_id")
                        callback(true, id, null)
                    } else {
                        val errorMessage = try {
                            JSONObject(bodyStr ?: "{}").optString("error", "Error desconocido")
                        } catch (e: Exception) {
                            "Error ${it.code}: ${it.message}"
                        }
                        callback(false, null, errorMessage)
                    }
                }
            }
        })
    }

    fun verificarTunel(
        nombre: String,
        password: String,
        alias: String,
        callback: (success: Boolean, tunnelId: Int?, errorMsg: String?) -> Unit
    ) {
        // Paso 1: obtener el túnel por nombre
        val url = "$BASE_URL/api/tunnels/get".toHttpUrl().newBuilder()
            .addQueryParameter("name", nombre)
            .build()

        val requestGet = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(requestGet).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, null, "Error de red al obtener túnel: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(false, null, "Túnel no encontrado")
                        return
                    }

                    val obj = JSONObject(it.body?.string() ?: "{}")
                    val tunnelId = obj.optInt("id", -1)
                    if (tunnelId == -1) {
                        callback(false, null, "ID del túnel inválido")
                        return
                    }

                    // Paso 2: hacer join con contraseña
                    val json = JSONObject()
                    json.put("tunnel_id", tunnelId)
                    json.put("password", password)
                    json.put("alias", alias)

                    val requestPost = Request.Builder()
                        .url("$BASE_URL/api/tunnels/join")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(requestPost).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            callback(false, null, "Error de red al unirse: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (it.isSuccessful) {
                                    callback(true, tunnelId, null)
                                } else {
                                    val msg = try {
                                        JSONObject(it.body?.string() ?: "{}")
                                            .optString("error", "Acceso denegado")
                                    } catch (e: Exception) {
                                        "Error ${it.code}: ${it.message}"
                                    }
                                    callback(false, null, msg)
                                }
                            }
                        }
                    })
                }
            }
        })
    }
}
