// crypto/Decryptor.kt
package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.PrivateKey

object Decryptor {

    /**
     * Descifra un archivo cifrado (.json), detectando el método usado.
     * @param inputFile Archivo cifrado
     * @param promptForPassword Función que retorna la contraseña ingresada por el usuario
     * @param privateKeyPEM Contenido PEM de la clave privada (solo si fue cifrado con llave)
     * @return Par de (bytes descifrados, extensión sugerida)
     */
    fun decryptFile(
        inputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String? = null
    ): Pair<ByteArray, String> {
        val json = JSONObject(inputFile.readText())

        val ext = json.optString("ext", "")
        val isPasswordEncrypted = json.has("salt_user") && json.has("encrypted_user_password")
        val isKeyEncrypted = json.has("key_user") && json.has("key_master")

        return when {
            isPasswordEncrypted -> {
                val result = PasswordCrypto.decryptFileWithPassword(inputFile, promptForPassword)
                Pair(result.decryptedBytes, ext)
            }

            isKeyEncrypted && privateKeyPEM != null -> {
                val privateKey = RSAUtils.loadPrivateKeyFromPEM(privateKeyPEM)

                val encryptedKeyUser = Base64.decode(json.getString("key_user"), Base64.NO_WRAP)
                val encryptedKeyMaster = Base64.decode(json.getString("key_master"), Base64.NO_WRAP)
                val encryptedData = Base64.decode(json.getString("data"), Base64.NO_WRAP)

                // Intentar con clave del usuario, luego con clave maestra
                val aesKey = try {
                    RSAUtils.decryptWithRSAOAEP(encryptedKeyUser, privateKey)
                } catch (e: Exception) {
                    RSAUtils.decryptWithRSAOAEP(encryptedKeyMaster, privateKey)
                }

                val aesKeySpec = javax.crypto.spec.SecretKeySpec(aesKey, "AES")
                val decryptedBytes = AESUtils.decryptAES(encryptedData, aesKeySpec)

                Pair(decryptedBytes, ext)
            }

            else -> {
                throw Exception("Archivo cifrado con formato no reconocido o incompleto.")
            }
        }
    }
}
