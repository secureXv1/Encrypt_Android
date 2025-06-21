package com.safeguard.encrypt_android.crypto

import android.util.Base64
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
     * Cifra un archivo con contraseña (PBKDF2 + AES-CBC).
     * Incluye salt y vector de inicialización (iv) concatenados y codificados.
     */
    fun encryptWithPassword(inputFile: File, password: String, outputFile: File) {
        val salt = CryptoUtils.generateRandomBytes(16)
        val iv = CryptoUtils.generateRandomBytes(16)
        val secretKey = CryptoUtils.deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val encrypted = cipher.doFinal(inputFile.readBytes())
        val json = JSONObject()

        json.put("key_user", Base64.encodeToString(salt + iv, Base64.NO_WRAP))  // salt + iv
        json.put("content", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        json.put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
        json.put("type", "password")

        outputFile.writeText(json.toString())
    }

    /**
     * Cifra un archivo con clave pública del usuario y del administrador.
     * El AES generado se cifra con ambas y se guarda junto con el contenido cifrado.
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
        json.put("key_user", Base64.encodeToString(encryptedKeyUser, Base64.NO_WRAP))
        json.put("key_master", Base64.encodeToString(encryptedKeyMaster, Base64.NO_WRAP))
        json.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
        json.put("content", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        json.put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
        json.put("type", "rsa")

        outputFile.writeText(json.toString())
    }
}
