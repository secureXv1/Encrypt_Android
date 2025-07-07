package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Primitive
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.asn1.pkcs.RSAPrivateKey as BcRSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey as BcRSAPublicKey


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
        val publicKey = decodeRSAPublicKeyPKCS1(publicKeyPEM)
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
        val privateKey = decodeRSAPrivateKeyPKCS1(privateKeyPEM)


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

    fun decodeRSAPublicKeyPKCS1(pem: String): PublicKey {
        val cleaned = pem
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val bytes = try {
            Base64.decode(cleaned, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw IllegalArgumentException("❌ La llave no está codificada en Base64 correctamente")
        }

        val bcKey = try {
            BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(bytes))
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalArgumentException("❌ Formato PKCS#1 inválido: ${e.message}")
        }

        val spec = RSAPublicKeySpec(bcKey.modulus, bcKey.publicExponent)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }



    fun decodeRSAPrivateKeyPKCS1(pem: String): PrivateKey {
        val cleanPem = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.decode(cleanPem, Base64.DEFAULT)
        val asn1 = ASN1InputStream(decoded).use { it.readObject() as BcRSAPrivateKey }

        val spec = RSAPrivateKeySpec(asn1.modulus, asn1.privateExponent)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    fun validateRSAPublicKeyPem(pem: String): Boolean {
        return try {
            val cleaned = pem
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace(Regex("\\s"), "") // quita todos los espacios, tabs y saltos

            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(bytes)) // si falla aquí, no es válida
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }







}
