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
     * Convierte ByteArray a hex (para compatibilidad con Windows)
     */
    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    /**
     * Cifra un archivo con contraseña (PBKDF2 + AES-CBC).
     * Serializa campos en HEX para que Windows pueda usar fromhex().
     */
    fun encryptWithPassword(inputFile: File, password: String, outputFile: File) {
        val salt = CryptoUtils.generateRandomBytes(16)
        val iv = CryptoUtils.generateRandomBytes(16)
        val secretKey = CryptoUtils.deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(inputFile.readBytes())
        val json = JSONObject()

        json.put("key_user", (salt + iv).toHexString())
        json.put("data", encrypted.toHexString())
        json.put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
        json.put("type", "password")

        outputFile.writeText(json.toString())
    }

    /**
     * Cifra un archivo con clave pública del usuario y del administrador.
     * Serializa claves y datos en HEX para Windows.
     */
    fun encryptWithPublicKey(inputFile: File, publicKeyPEM: String, outputFile: File) {
        val secretKey = CryptoUtils.generateAESKey()
        val iv = CryptoUtils.generateRandomBytes(16)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(inputFile.readBytes())
        val encryptedKeyUser = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, publicKeyPEM)
        val encryptedKeyMaster = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, MasterKey.PUBLIC_KEY_PEM)

        val json = JSONObject()
        json.put("key_user", encryptedKeyUser.toHexString())
        json.put("key_master", encryptedKeyMaster.toHexString())
        json.put("iv", iv.toHexString())
        json.put("data", encrypted.toHexString())
        json.put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
        json.put("type", "rsa")

        outputFile.writeText(json.toString())
    }
}
