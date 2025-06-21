// crypto/PasswordCrypto.kt
package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

object PasswordCrypto {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 32

    private val masterPassword = "SeguraAdmin123!".toCharArray()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH * 8)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encryptFileWithPassword(inputFile: File, password: String, outputFile: File) {
        val fileBytes = inputFile.readBytes()
        val ext = inputFile.extension
        val originalPayload = JSONObject(
            mapOf(
                "ext" to ".$ext",
                "content" to Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            )
        )
        val serialized = originalPayload.toString().toByteArray()

        // === Clave para el usuario ===
        val saltUser = generateSalt()
        val keyUser = deriveKey(password.toCharArray(), saltUser)

        val encryptedData = AESUtils.encryptAES(serialized, keyUser)

        // === Clave para el administrador ===
        val saltAdmin = generateSalt()
        val keyAdmin = deriveKey(masterPassword, saltAdmin)

        val encryptedPasswordBytes = AESUtils.encryptAES(password.toByteArray(), keyAdmin)

        // === Serializar salida ===
        val json = JSONObject(
            mapOf(
                "salt_user" to Base64.encodeToString(saltUser, Base64.NO_WRAP),
                "salt_admin" to Base64.encodeToString(saltAdmin, Base64.NO_WRAP),
                "encrypted_user_password" to Base64.encodeToString(encryptedPasswordBytes, Base64.NO_WRAP),
                "data" to Base64.encodeToString(encryptedData, Base64.NO_WRAP),
                "ext" to ".$ext"
            )
        )

        outputFile.writeText(json.toString())
    }

    data class PasswordDecryptionResult(
        val decryptedBytes: ByteArray,
        val recoveredPassword: String? = null
    )

    fun decryptFileWithPassword(
        inputFile: File,
        promptForPassword: () -> String,
        adminPrivatePassword: String = String(masterPassword)
    ): PasswordDecryptionResult {
        val json = JSONObject(inputFile.readText())

        val saltUser = Base64.decode(json.getString("salt_user"), Base64.NO_WRAP)
        val saltAdmin = Base64.decode(json.getString("salt_admin"), Base64.NO_WRAP)
        val encryptedPasswordBytes = Base64.decode(json.getString("encrypted_user_password"), Base64.NO_WRAP)
        val encryptedData = Base64.decode(json.getString("data"), Base64.NO_WRAP)

        var attempts = 0
        while (attempts < 3) {
            val inputPassword = promptForPassword()
            val keyUser = deriveKey(inputPassword.toCharArray(), saltUser)

            try {
                val decrypted = AESUtils.decryptAES(encryptedData, keyUser)
                return PasswordDecryptionResult(decrypted) // ✅ Correcta
            } catch (e: Exception) {
                // Falló: intentamos como admin
                try {
                    val keyAdmin = deriveKey(adminPrivatePassword.toCharArray(), saltAdmin)
                    val recoveredPasswordBytes = AESUtils.decryptAES(encryptedPasswordBytes, keyAdmin)
                    val recoveredPassword = String(recoveredPasswordBytes)

                    val keyUserReal = deriveKey(recoveredPassword.toCharArray(), saltUser)
                    val decrypted = AESUtils.decryptAES(encryptedData, keyUserReal)
                    return PasswordDecryptionResult(decrypted, recoveredPassword)
                } catch (ex: Exception) {
                    attempts++
                }
            }
        }

        throw Exception("No se pudo descifrar con contraseña ni como administrador.")
    }
}
