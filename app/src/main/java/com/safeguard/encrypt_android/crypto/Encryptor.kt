package com.safeguard.encrypt_android.crypto

import android.os.Environment
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object Encryptor {

    enum class Metodo {
        PASSWORD,
        RSA
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    private fun getOutputFile(baseName: String): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${baseName}_cif.json")
    }

    fun encryptWithPassword(inputFile: File, password: String): File {
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
            put("filename", inputFile.name)
            put("type", "password")
        }

        val outputFile = getOutputFile(inputFile.nameWithoutExtension)
        outputFile.writeText(json.toString())
        return outputFile
    }

    fun encryptWithPublicKey(inputFile: File, publicKeyPEM: String): File {
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
            put("filename", inputFile.name)
            put("type", "rsa")
        }

        val outputFile = getOutputFile(inputFile.nameWithoutExtension)
        outputFile.writeText(json.toString())
        return outputFile
    }
}
