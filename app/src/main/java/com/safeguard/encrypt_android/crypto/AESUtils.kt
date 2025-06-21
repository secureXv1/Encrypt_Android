// crypto/AESUtils.kt
package com.safeguard.encrypt_android.crypto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object AESUtils {

    // Crea una clave AES válida (256 bits) desde un array de bytes
    fun getAesKey(raw: ByteArray): SecretKey {
        return SecretKeySpec(raw, "AES")
    }

    // Cifra datos usando AES (modo ECB + PKCS5)
    fun encryptAES(plainBytes: ByteArray, aesKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        return cipher.doFinal(plainBytes)
    }

    // Descifra datos usando AES
    fun decryptAES(encryptedBytes: ByteArray, aesKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey)
        return cipher.doFinal(encryptedBytes)
    }

    // Funciones utilitarias para codificar en base64 (por si se requiere)
    fun encodeBase64(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP)
    }

    fun decodeBase64(input: String): ByteArray {
        return Base64.decode(input, Base64.NO_WRAP)
    }
}
