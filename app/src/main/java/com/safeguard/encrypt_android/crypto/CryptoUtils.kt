package com.safeguard.encrypt_android.crypto

import android.util.Base64
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    // Genera una clave AES aleatoria (256 bits)
    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // Cambiar a 128 si el dispositivo no soporta 256
        return keyGen.generateKey()
    }

    // Genera un arreglo de bytes aleatorios (usado para IVs o salt)
    fun generateRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    // Deriva una clave AES a partir de una contraseña + salt usando PBKDF2
    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    // Cifra una clave AES con una clave pública RSA
    fun encryptKeyWithPublicKey(secretKey: ByteArray, publicKeyPEM: String): ByteArray {
        val publicKey = decodePublicKey(publicKeyPEM)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(secretKey)
    }

    // Descifra una clave AES con una clave privada RSA
    fun decryptKeyWithPrivateKey(encryptedKey: ByteArray, privateKeyPEM: String): ByteArray {
        val privateKey = decodePrivateKey(privateKeyPEM)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(encryptedKey)
    }

    // Decodifica una clave pública PEM (formato X.509)
    private fun decodePublicKey(pem: String): PublicKey {
        val clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    // Decodifica una clave privada PEM (formato PKCS8)
    private fun decodePrivateKey(pem: String): PrivateKey {
        val clean = pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    // Descifra contenido usando AES CBC con clave derivada desde contraseña
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
