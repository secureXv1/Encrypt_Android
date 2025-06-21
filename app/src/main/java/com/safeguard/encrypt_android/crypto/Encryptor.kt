package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object Encryptor {

    enum class Metodo {
        PASSWORD, RSA
    }

    /**
     * Cifra un archivo con contraseña.
     * Usa doble cifrado: contenido + contraseña protegida por el administrador.
     */
    fun encryptWithPassword(inputFile: File, password: String, outputFile: File) {
        PasswordCrypto.encryptFileWithPassword(inputFile, password, outputFile)
    }

    /**
     * Cifra un archivo con clave pública del destinatario y clave maestra.
     */
    fun encryptWithPublicKey(inputFile: File, userPublicKeyPEM: String, outputFile: File) {
        val fileBytes = inputFile.readBytes()
        val ext = inputFile.extension

        // Serializar contenido original
        val originalPayload = JSONObject(
            mapOf(
                "ext" to ".$ext",
                "content" to Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            )
        )
        val serialized = originalPayload.toString().toByteArray()

        // 1️⃣ Generar clave AES
        val aesKeyBytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(aesKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // 2️⃣ Cifrar datos
        val encryptedData = AESUtils.encryptAES(serialized, aesKey)

        // 3️⃣ Cargar claves públicas
        val userPubKey = RSAUtils.loadPublicKeyFromPEM(userPublicKeyPEM)
        val masterPubKey = RSAUtils.loadPublicKeyFromPEM(MasterKey.PUBLIC_KEY_PEM)

        // 4️⃣ Cifrar la clave AES con ambas
        val encryptedKeyUser = RSAUtils.encryptWithRSAOAEP(aesKeyBytes, userPubKey)
        val encryptedKeyMaster = RSAUtils.encryptWithRSAOAEP(aesKeyBytes, masterPubKey)

        // 5️⃣ Serializar resultado
        val json = JSONObject(
            mapOf(
                "key_user" to Base64.encodeToString(encryptedKeyUser, Base64.NO_WRAP),
                "key_master" to Base64.encodeToString(encryptedKeyMaster, Base64.NO_WRAP),
                "data" to Base64.encodeToString(encryptedData, Base64.NO_WRAP),
                "ext" to ".$ext"
            )
        )

        outputFile.writeText(json.toString())
    }
}
