package com.safeguard.encrypt_android.crypto

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Decryptor {
    @OptIn(ExperimentalStdlibApi::class)
    fun decryptFile(
        inputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String?,
        allowAdminRecovery: Boolean = false
    ): Pair<ByteArray, String> {
        val json = JSONObject(inputFile.readText())
        val type = json.optString("type", "password")
        val encryptedBytes = json.getString("data").hexToByteArray()
        Log.d("Decryptor", "🔍 Primeros 32 bytes de 'data': ${encryptedBytes.take(32).joinToString(" ") { "%02x".format(it) }}")

        val decryptedBytes = when (type.lowercase()) {
            "rsa" -> {
                if (privateKeyPEM.isNullOrBlank()) {
                    throw IllegalArgumentException("❌ Se requiere una clave privada.")
                }

                val encryptedKeyUser = json.getString("key_user").hexToByteArray()
                val iv = json.getString("iv").hexToByteArray()
                Log.d("Decryptor", "🔐 [RSA] IV length: ${iv.size}, Data length: ${encryptedBytes.size}")

                val keyBytes = try {
                    CryptoUtils.decryptKeyWithPrivateKey(encryptedKeyUser, privateKeyPEM)
                } catch (e: Exception) {
                    val encryptedKeyMaster = json.optString("key_master", "")
                    if (encryptedKeyMaster.isEmpty()) {
                        throw IllegalArgumentException("❌ Clave maestra no disponible.")
                    }
                    CryptoUtils.decryptKeyWithPrivateKey(
                        encryptedKeyMaster.hexToByteArray(),
                        MasterKey.PRIVATE_KEY_PEM
                    )
                }

                decryptGCM(keyBytes, iv, encryptedBytes)
            }

            "password" -> {
                val password = promptForPassword()
                val saltUser = json.getString("salt_user").hexToByteArray()
                val ivUser = json.getString("iv_user").hexToByteArray()
                Log.d("Decryptor", "🔐 [PWD] IV length: ${ivUser.size}, Data length: ${encryptedBytes.size}")

                try {
                    CryptoUtils.decryptWithPasswordCBC(encryptedBytes, password, saltUser, ivUser)
                }
                catch (e: Exception) {
                    Log.e("Decryptor", "❌ Descifrado con contraseña falló: ${e.message}")
                    if (!allowAdminRecovery) throw IllegalArgumentException("❌ Contraseña incorrecta.")

                    try {
                        val saltAdmin = json.getString("salt_admin").hexToByteArray()
                        val ivAdmin = json.getString("iv_admin").hexToByteArray()
                        val encryptedPassword = json.getString("encrypted_user_password").hexToByteArray()

                        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)
                        val cipherAdmin = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipherAdmin.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(keyAdmin.encoded, "AES"),
                            IvParameterSpec(ivAdmin)
                        )

                        val recoveredPassword = cipherAdmin.doFinal(encryptedPassword).toString(Charsets.UTF_8)
                        Log.d("Decryptor", "✅ Contraseña recuperada: $recoveredPassword")

                        CryptoUtils.decryptWithPasswordCBC(encryptedBytes, recoveredPassword, saltUser, ivUser)



                    } catch (ex: Exception) {
                        Log.e("Decryptor", "❌ Recuperación con clave maestra fallida: ${ex.message}")
                        throw IllegalArgumentException("❌ No se pudo recuperar la contraseña.")
                    }
                }
            }

            else -> throw IllegalArgumentException("❌ Tipo de archivo no soportado: $type")
        }

        // 🔍 Extraer si el contenido es JSON base64
        val resultadoFinal: Pair<ByteArray, String> = try {
            val interno = JSONObject(String(decryptedBytes))
            val nombre = interno.optString("filename", "archivo")
            val base64 = interno.optString("content", "")
            val ext = interno.optString("ext", ".bin").removePrefix(".")
            val decoded = Base64.decode(base64, Base64.NO_WRAP)
            Pair(decoded, "$nombre.dec.$ext")
        } catch (_: Exception) {
            val filename = json.optString("filename", "archivo")
            val ext = json.optString("ext", "bin").removePrefix(".")
            Pair(decryptedBytes, "${filename}_dec.$ext")
        }

        return resultadoFinal
    }

    private fun decryptGCM(keyBytes: ByteArray, iv: ByteArray, encrypted: ByteArray): ByteArray {
        if (encrypted.size < 32) {
            throw IllegalArgumentException("❌ Archivo GCM inválido: muy corto.")
        }

        Log.d("Decryptor", "📦 GCM: total=${encrypted.size}, iv=${iv.size}, key=${keyBytes.size}")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), gcmSpec)
        return cipher.doFinal(encrypted) // ✅ combinado: ciphertext + tag
    }

    private fun decryptCBC(keyBytes: ByteArray, iv: ByteArray, encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

}
