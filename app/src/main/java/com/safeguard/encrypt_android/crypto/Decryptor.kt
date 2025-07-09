package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKey
import com.safeguard.encrypt_android.crypto.MasterKey
import javax.crypto.spec.IvParameterSpec

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
    ): Pair<ByteArray, String> {
        val json = JSONObject(inputFile.readText())
        val type = json.optString("type", "password")

        val encryptedBytes = json.getString("data").hexToByteArray()

        val decryptedBytes = when (type.lowercase()) {
            "rsa" -> {
                if (privateKeyPEM.isNullOrBlank()) {
                    throw IllegalArgumentException("❌ Se requiere una clave privada para este archivo.")
                }

                val encryptedKeyUserHex = json.getString("key_user")
                val encryptedKeyMasterHex = json.optString("key_master", "")
                val ivHex = json.getString("iv").hexToByteArray()

                val aesKeyBytes: ByteArray = try {
                    // Intentar con la clave privada del usuario
                    CryptoUtils.decryptKeyWithPrivateKey(
                        encryptedKeyUserHex.hexToByteArray(),
                        privateKeyPEM
                    )
                } catch (e: Exception) {
                    try {
                        if (encryptedKeyMasterHex.isBlank()) throw Exception("❌ Clave maestra no disponible.")
                        CryptoUtils.decryptKeyWithPrivateKey(
                            encryptedKeyMasterHex.hexToByteArray(),
                            MasterKey.PRIVATE_KEY_PEM
                        )
                    } catch (ex: Exception) {
                        throw IllegalArgumentException("❌ Clave privada incorrecta. No coincide con ninguna clave usada.")
                    }
                }

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, ivHex)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKeyBytes, "AES"), spec)
                cipher.doFinal(encryptedBytes)
            }

            "password" -> {
                val passwordIngresada = promptForPassword()

                val saltUser = json.getString("salt_user").hexToByteArray()
                val ivUser = json.getString("iv_user").hexToByteArray()

                val claveParaIntentar: SecretKey = CryptoUtils.deriveKeyFromPassword(passwordIngresada, saltUser)

                try {
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, claveParaIntentar, IvParameterSpec(ivUser))
                    cipher.doFinal(encryptedBytes)
                } catch (e: Exception) {
                    if (!allowAdminRecovery) {
                        throw IllegalArgumentException("❌ Contraseña incorrecta.")
                    }

                    try {
                        val saltAdmin = json.getString("salt_admin").hexToByteArray()
                        val ivAdmin = json.getString("iv_admin").hexToByteArray()
                        val encryptedPassword = json.getString("encrypted_user_password").hexToByteArray()

                        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)
                        val cipherAdmin = Cipher.getInstance("AES/GCM/NoPadding")
                        val specAdmin = GCMParameterSpec(128, ivAdmin)
                        cipherAdmin.init(Cipher.DECRYPT_MODE, keyAdmin, specAdmin)

                        val recoveredPassword = cipherAdmin.doFinal(encryptedPassword).toString(Charsets.UTF_8)

                        val keyUser = CryptoUtils.deriveKeyFromPassword(recoveredPassword, saltUser)

                        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipher.init(Cipher.DECRYPT_MODE, keyUser, IvParameterSpec(ivUser))
                        cipher.doFinal(encryptedBytes)
                    } catch (ex: Exception) {
                        throw IllegalArgumentException("❌ Recuperación con clave maestra fallida.")
                    }
                }
            }


            else -> throw IllegalArgumentException("❌ Tipo de archivo cifrado desconocido: $type")
        }

        val filenameBase = json.optString("filename", "archivo")
        val extension = json.optString("ext", "bin").removePrefix(".")
        val nombreFinal = "${filenameBase}_dec.$extension"


        return Pair(decryptedBytes, nombreFinal)
    }
}
