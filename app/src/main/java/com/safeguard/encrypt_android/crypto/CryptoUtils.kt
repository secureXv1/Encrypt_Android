package com.safeguard.encrypt_android.crypto

import android.util.Base64
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun generateRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    // ✅ NUEVO: Descifrado con AES-GCM usando contraseña derivada
    fun decryptWithPasswordGCM(
        encryptedData: ByteArray,
        salt: ByteArray,
        password: String,
        iv: ByteArray
    ): ByteArray {
        val secretKey = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encryptedData)
    }

    fun encryptKeyWithPublicKey(secretKey: ByteArray, publicKeyPEM: String): ByteArray {
        val publicKey = decodePublicKey(publicKeyPEM)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(secretKey)
    }

    fun decryptKeyWithPrivateKey(encryptedKey: ByteArray, privateKeyPEM: String): ByteArray {
        val cleanedPem = privateKeyPEM
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.decode(cleanedPem, Base64.DEFAULT)

        // Convertir PKCS#1 → PrivateKey
        val spec = PKCS1EncodedKeySpec(keyBytes).toPKCS8()
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(spec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(encryptedKey)
    }


    private fun decodePublicKey(pem: String): PublicKey {
        val clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private fun decodePrivateKey(pem: String): PrivateKey {
        val clean = pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    // ❌ OPCIONAL: Ya no se necesita si solo se usa GCM
    @Deprecated("Usa GCM en lugar de CBC", ReplaceWith("decryptWithPasswordGCM(...)"))
    fun decryptWithPassword(
        encryptedData: ByteArray,
        salt: ByteArray,
        password: String,
        iv: ByteArray
    ): ByteArray {
        val secretKey = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(encryptedData)
    }
}
