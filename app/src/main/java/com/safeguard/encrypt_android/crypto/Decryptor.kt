package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKey

object Decryptor {

    private fun String.hexToByteArray(): ByteArray {
        val len = this.length
        require(len % 2 == 0) { "Hex string must have even length" }
        return ByteArray(len / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun decryptFile(
        inputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String?,
        allowAdminRecovery: Boolean = false
    ): Pair<ByteArray, String>
    {
        val json = JSONObject(inputFile.readText())
        val ext = json.optString("ext", ".bin")
        val type = json.optString("type", "password")
        val filenameOriginal = json.optString("filename", "archivo" + ext)

        val encryptedBytes = json.getString("data").hexToByteArray()

        val decryptedBytes = when (type.lowercase()) {
            "rsa" -> {
                if (privateKeyPEM.isNullOrBlank()) {
                    throw IllegalArgumentException("❌ Se requiere una clave privada para este archivo.")
                }

                val encryptedKeyHex = json.getString("key_user")
                val ivHex = json.getString("iv")

                val aesKeyBytes = CryptoUtils.decryptKeyWithPrivateKey(
                    encryptedKeyHex.hexToByteArray(),
                    privateKeyPEM
                )

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, ivHex.hexToByteArray())
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(aesKeyBytes, "AES"),
                    spec
                )
                cipher.doFinal(encryptedBytes)
            }

            "password" -> {
                val passwordIngresada = promptForPassword()

                val saltUser = Base64.decode(json.getString("salt_user"), Base64.NO_WRAP)
                val ivUser = json.getString("iv_user").hexToByteArray()

                val claveParaIntentar: SecretKey? = try {
                    CryptoUtils.deriveKeyFromPassword(passwordIngresada, saltUser)
                } catch (_: Exception) {
                    null
                }

                try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(128, ivUser)
                    cipher.init(Cipher.DECRYPT_MODE, claveParaIntentar, spec)
                    cipher.doFinal(encryptedBytes)
                } catch (e: Exception) {
                    if (passwordIngresada != "SeguraAdmin123!") {
                        throw IllegalArgumentException("❌ Contraseña incorrecta.")
                    }

                    try {
                        val saltAdmin = Base64.decode(json.getString("salt_admin"), Base64.NO_WRAP)
                        val ivAdmin = json.getString("iv_admin").hexToByteArray()
                        val encryptedPassword = json.getString("encrypted_user_password").hexToByteArray()

                        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)
                        val cipherAdmin = Cipher.getInstance("AES/GCM/NoPadding")
                        val specAdmin = GCMParameterSpec(128, ivAdmin)
                        cipherAdmin.init(Cipher.DECRYPT_MODE, keyAdmin, specAdmin)

                        val recoveredPassword = cipherAdmin.doFinal(encryptedPassword).toString(Charsets.UTF_8)

                        val keyUser = CryptoUtils.deriveKeyFromPassword(recoveredPassword, saltUser)

                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        val spec = GCMParameterSpec(128, ivUser)
                        cipher.init(Cipher.DECRYPT_MODE, keyUser, spec)
                        cipher.doFinal(encryptedBytes)
                    } catch (ex: Exception) {
                        throw IllegalArgumentException("❌ Recuperación con clave maestra fallida.")
                    }
                }
            }




            else -> throw IllegalArgumentException("❌ Tipo de archivo cifrado desconocido: $type")
        }

        val filenameSinExt = filenameOriginal.substringBeforeLast(".")
        val extension = filenameOriginal.substringAfterLast(".", "")
        val nombreFinal = "${filenameSinExt}_dec.${extension}"

        return Pair(decryptedBytes, nombreFinal)
    }

}
