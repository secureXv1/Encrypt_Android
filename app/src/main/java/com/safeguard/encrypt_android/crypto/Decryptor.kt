// crypto/Decryptor.kt
package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.PrivateKey

object Decryptor {

    fun decryptFile(
        inputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String? = null
    ): Pair<ByteArray, String> {
        val json = JSONObject(inputFile.readText())
        val ext = json.optString("ext", ".bin")

        val content = Base64.decode(json.getString("content"), Base64.DEFAULT)

        return if (privateKeyPEM != null) {
            val encryptedKey = Base64.decode(json.getString("key_user"), Base64.DEFAULT)
            val iv = Base64.decode(json.getString("iv"), Base64.DEFAULT)

            val aesKeyBytes = CryptoUtils.decryptKeyWithPrivateKey(encryptedKey, privateKeyPEM)
            val secretKey = SecretKeySpec(aesKeyBytes, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(content)

            Pair(decrypted, ext)

        } else {
            val password = promptForPassword()
            val keyIvCombined = Base64.decode(json.getString("key_user"), Base64.DEFAULT)
            val salt = keyIvCombined.sliceArray(0 until 16)
            val iv = keyIvCombined.sliceArray(16 until 32)
            val secretKey = CryptoUtils.deriveKeyFromPassword(password, salt)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(content)

            Pair(decrypted, ext)
        }
    }
}
