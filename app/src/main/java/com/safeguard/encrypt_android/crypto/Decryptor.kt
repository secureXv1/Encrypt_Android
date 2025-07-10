package com.safeguard.encrypt_android.crypto

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
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

        val decryptedBytes = when (type.lowercase()) {
            "rsa" -> {
                if (privateKeyPEM.isNullOrBlank()) {
                    throw IllegalArgumentException("❌ Se requiere una clave privada.")
                }

                val encryptedKeyUser = json.getString("key_user").hexToByteArray()
                val iv = json.getString("iv").hexToByteArray()
                val keyBytes: ByteArray = try {
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

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val ivSpec = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), ivSpec)
                cipher.doFinal(encryptedBytes)
            }

            "password" -> {
                val password = promptForPassword()
                val saltUser = json.getString("salt_user").hexToByteArray()
                val ivUser = json.getString("iv_user").hexToByteArray()
                val userKey = CryptoUtils.deriveKeyFromPassword(password, saltUser)

                try {
                    Log.d("Decryptor", "🔓 Intentando descifrado con contraseña proporcionada...")
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(userKey.encoded, "AES"), IvParameterSpec(ivUser))
                    cipher.doFinal(encryptedBytes)
                } catch (e: Exception) {
                    Log.e("Decryptor", "❌ Descifrado con contraseña falló: ${e.message}")
                    if (!allowAdminRecovery) throw IllegalArgumentException("❌ Contraseña incorrecta.")

                    try {
                        val saltAdmin = json.getString("salt_admin").hexToByteArray()
                        val ivAdmin = json.getString("iv_admin").hexToByteArray()
                        val encryptedPassword = json.getString("encrypted_user_password").hexToByteArray()

                        val adminKey = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)
                        val cipherAdmin = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipherAdmin.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(adminKey.encoded, "AES"),
                            IvParameterSpec(ivAdmin)
                        )

                        val recoveredPassword = cipherAdmin.doFinal(encryptedPassword).toString(Charsets.UTF_8)
                        Log.d("Decryptor", "✅ Contraseña recuperada: $recoveredPassword")

                        val recoveredKey = CryptoUtils.deriveKeyFromPassword(recoveredPassword, saltUser)
                        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipher.init(Cipher.DECRYPT_MODE, recoveredKey, IvParameterSpec(ivUser))
                        cipher.doFinal(encryptedBytes)
                    } catch (ex: Exception) {
                        Log.e("Decryptor", "❌ Recuperación con clave maestra fallida: ${ex.message}")
                        throw IllegalArgumentException("❌ No se pudo recuperar la contraseña.")
                    }
                }
            }

            else -> throw IllegalArgumentException("❌ Tipo de archivo no soportado: $type")
        }

        val filename = json.optString("filename", "archivo")
        val ext = json.optString("ext", "bin").removePrefix(".")
        val suggestedName = "${filename}_dec.$ext"
        return Pair(decryptedBytes, suggestedName)
    }
}
