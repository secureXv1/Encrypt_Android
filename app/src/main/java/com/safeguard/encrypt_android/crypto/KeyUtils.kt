package com.safeguard.encrypt_android.crypto

import android.util.Base64
import org.bouncycastle.asn1.pkcs.RSAPrivateKey as BcRSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey as BcRSAPublicKey
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.pkcs.RSAPublicKey
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.security.*
import java.security.spec.*

object KeyUtils {

    fun generateRSAKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    fun encodePrivateKeyToPKCS1Pem(key: PrivateKey): String {
        val rsaKey = key as java.security.interfaces.RSAPrivateCrtKey
        val bcKey = org.bouncycastle.asn1.pkcs.RSAPrivateKey(
            rsaKey.modulus,
            rsaKey.publicExponent,
            rsaKey.privateExponent,
            rsaKey.primeP,
            rsaKey.primeQ,
            rsaKey.primeExponentP,
            rsaKey.primeExponentQ,
            rsaKey.crtCoefficient
        )
        val der = bcKey.encoded
        val base64 = Base64.encodeToString(der, Base64.NO_WRAP)
        return wrapAsPem(base64, "RSA PRIVATE KEY")
    }



    fun encodePublicKeyToPKCS1Pem(publicKey: PublicKey): String {
        val rsaPublicKey = publicKey as java.security.interfaces.RSAPublicKey
        val bcKey = BcRSAPublicKey(rsaPublicKey.modulus, rsaPublicKey.publicExponent)
        val der = bcKey.encoded
        val base64 = Base64.encodeToString(der, Base64.NO_WRAP)
        return wrapAsPem(base64, "RSA PUBLIC KEY")
    }




    fun decodePrivateKeyFromPKCS1Pem(pem: String): PrivateKey {
        val cleaned = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val pkcs1Bytes = Base64.decode(cleaned, Base64.NO_WRAP)

        // Envolver PKCS#1 en PKCS#8 para Java KeyFactory
        val pkcs8Bytes = wrapRsaPkcs1ToPkcs8(pkcs1Bytes)
        val spec = PKCS8EncodedKeySpec(pkcs8Bytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    fun decodePublicKeyFromPKCS1Pem(pem: String): PublicKey {
        val cleaned = pem
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val pkcs1Bytes = Base64.decode(cleaned, Base64.NO_WRAP)

        val bcKey = BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(pkcs1Bytes))
        val spec = RSAPublicKeySpec(bcKey.modulus, bcKey.publicExponent)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    fun isValidPkcs1PublicKey(pem: String): Boolean {
        return try {
            val cleaned = pem
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")
            val decoded = Base64.decode(cleaned, Base64.NO_WRAP)
            BcRSAPublicKey.getInstance(ASN1Primitive.fromByteArray(decoded))
            true
        } catch (e: Exception) {
            false
        }
    }


    fun wrapRsaPkcs1ToPkcs8(pkcs1: ByteArray): ByteArray {
        val pkcs8Header = byteArrayOf(
            0x30.toByte(), 0x82.toByte(), // SEQUENCE + length
            0x00, 0x00, // (placeholder)
            0x02, 0x01, 0x00, // Version
            0x30, 0x0D, // SEQUENCE
            0x06, 0x09, // OID
            0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01,
            0x05, 0x00,
            0x04, 0x82.toByte(), // OCTET STRING + length
            0x00, 0x00 // (placeholder)
        )
        val totalLen = pkcs1.size + 26
        pkcs8Header[2] = ((totalLen - 4) shr 8).toByte()
        pkcs8Header[3] = ((totalLen - 4) and 0xFF).toByte()
        pkcs8Header[24] = (pkcs1.size shr 8).toByte()
        pkcs8Header[25] = (pkcs1.size and 0xFF).toByte()

        return pkcs8Header + pkcs1
    }

    fun wrapAsPem(base64: String, title: String): String {
        return """
            -----BEGIN $title-----
            $base64
            -----END $title-----
        """.trimIndent()
    }
}
