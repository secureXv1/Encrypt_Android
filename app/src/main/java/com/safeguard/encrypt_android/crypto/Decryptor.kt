package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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
        privateKeyPEM: String? = null
    ): Pair<ByteArray, String> {
        val json = JSONObject(inputFile.readText())
        val ext = json.optString("ext", ".bin")
        val type = json.optString("type", "password")
        val filenameOriginal = json.optString("filename", "archivo" + ext)

        // 🔹 Lectura opcional del campo created_by (no se usa por ahora)
        val createdBy = json.optString("created_by", null)

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

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(aesKeyBytes, "AES"),
                    IvParameterSpec(ivHex.hexToByteArray())
                )
                cipher.doFinal(encryptedBytes)
            }

            "password" -> {
                val password = promptForPassword()

                try {
                    // Intentar como usuario
                    val saltUser = Base64.decode(json.getString("salt_user"), Base64.DEFAULT)
                    val ivUser = json.getString("iv_user").hexToByteArray()
                    val keyUser = CryptoUtils.deriveKeyFromPassword(password, saltUser)

                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, keyUser, IvParameterSpec(ivUser))
                    cipher.doFinal(encryptedBytes)

                } catch (e: Exception) {
                    // Intentar como administrador
                    try {
                        val saltAdmin = Base64.decode(json.getString("salt_admin"), Base64.DEFAULT)
                        val ivAdmin = json.getString("iv_admin").hexToByteArray()
                        val encryptedPasswordHex = json.getString("encrypted_user_password").hexToByteArray()

                        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)

                        val cipherAdmin = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipherAdmin.init(Cipher.DECRYPT_MODE, keyAdmin, IvParameterSpec(ivAdmin))
                        val recoveredPassword = cipherAdmin.doFinal(encryptedPasswordHex).toString(Charsets.UTF_8)

                        // Reintentar con la contraseña del usuario real
                        val saltUser = Base64.decode(json.getString("salt_user"), Base64.DEFAULT)
                        val ivUser = json.getString("iv_user").hexToByteArray()
                        val keyUser = CryptoUtils.deriveKeyFromPassword(recoveredPassword, saltUser)

                        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipher.init(Cipher.DECRYPT_MODE, keyUser, IvParameterSpec(ivUser))
                        cipher.doFinal(encryptedBytes)

                    } catch (ex: Exception) {
                        throw IllegalArgumentException("❌ Contraseña incorrecta o archivo dañado.")
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
