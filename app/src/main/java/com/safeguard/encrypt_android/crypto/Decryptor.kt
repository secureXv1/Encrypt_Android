package com.safeguard.encrypt_android.crypto

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

        val encryptedHex = when {
            json.has("data") -> json.getString("data")
            json.has("content") -> json.getString("content")
            else -> throw IllegalArgumentException("❌ No se encontró campo 'data' ni 'content'")
        }

        val encryptedBytes = encryptedHex.hexToByteArray()

        return when (type) {
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

                val secretKey = SecretKeySpec(aesKeyBytes, "AES")
                val iv = ivHex.hexToByteArray()

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
                val decrypted = cipher.doFinal(encryptedBytes)

                Pair(decrypted, ext)
            }

            "password" -> {
                val password = promptForPassword()
                val keyIvCombined = json.getString("key_user").hexToByteArray()
                val salt = keyIvCombined.sliceArray(0 until 16)
                val iv = keyIvCombined.sliceArray(16 until 32)

                val secretKey = CryptoUtils.deriveKeyFromPassword(password, salt)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
                val decrypted = cipher.doFinal(encryptedBytes)

                Pair(decrypted, ext)
            }

            else -> throw IllegalArgumentException("❌ Tipo de archivo cifrado desconocido: $type")
        }
    }
}
