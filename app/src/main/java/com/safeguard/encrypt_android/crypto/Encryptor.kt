package com.safeguard.encrypt_android.crypto

import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encryptor {

    enum class Metodo {
        PASSWORD,
        RSA
    }

    /**
     * Convierte ByteArray a string hexadecimal.
     */
    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    /**
     * Cifra un archivo con contraseña utilizando AES-CBC y derivación PBKDF2.
     * Guarda la salida en formato HEX compatible con Windows (fromhex()).
     */
    fun encryptWithPassword(inputFile: File, password: String, outputFile: File) {
        val salt = CryptoUtils.generateRandomBytes(16)
        val iv = CryptoUtils.generateRandomBytes(16)
        val secretKey = CryptoUtils.deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(inputFile.readBytes())
        val json = JSONObject().apply {
            put("key_user", (salt + iv).toHexString())
            put("data", encrypted.toHexString())
            put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
            put("type", "password")
        }

        outputFile.writeText(json.toString())
    }

    /**
     * Cifra un archivo con clave pública del usuario y la clave pública maestra (admin).
     * Los datos se cifran con AES-CBC y la clave AES se cifra con RSA.
     * Todo se serializa en HEX para mantener compatibilidad con sistemas que usan .hex().
     */
    fun encryptWithPublicKey(inputFile: File, publicKeyPEM: String, outputFile: File) {
        val secretKey = CryptoUtils.generateAESKey()
        val iv = CryptoUtils.generateRandomBytes(16)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(inputFile.readBytes())

        val encryptedKeyUser = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, publicKeyPEM)
        val encryptedKeyMaster = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, MasterKey.PUBLIC_KEY_PEM)

        val json = JSONObject().apply {
            put("key_user", encryptedKeyUser.toHexString())
            put("key_master", encryptedKeyMaster.toHexString())
            put("iv", iv.toHexString())
            put("data", encrypted.toHexString())
            put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
            put("type", "rsa")
        }

        outputFile.writeText(json.toString())
    }
}
