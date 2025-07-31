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

    fun encryptKeyWithPublicKey(aesKey: ByteArray, pem: String): ByteArray {
        val publicKey = if (pem.contains("-----BEGIN RSA PUBLIC KEY-----")) {
            decodeRSAPublicKeyPKCS1(pem)
        } else {
            val cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace(Regex("\\s"), "")
            val keyBytes = Base64.decode(cleaned, Base64.DEFAULT)
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
        }

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(aesKey)
    }




    fun decryptKeyWithPrivateKey(encryptedKey: ByteArray, privateKeyPEM: String): ByteArray {
        val privateKey = decodeRSAPrivateKeyPKCS1(privateKeyPEM)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(encryptedKey)
    }

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

        val bytes = Base64.decode(cleaned, Base64.NO_WRAP)
        val bcKey = BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(bytes))
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
                .replace("\\s".toRegex(), "")

            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(bytes))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    fun convertPkcs1ToPkcs8UsingBC(pkcs1: ByteArray): ByteArray {
        val spec = BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(pkcs1))
        val rsaSpec = RSAPublicKeySpec(spec.modulus, spec.publicExponent)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(rsaSpec)
        return publicKey.encoded // Esta es la forma correcta y segura
    }

}
