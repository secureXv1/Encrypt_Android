// crypto/KeyUtils.kt
package com.safeguard.encrypt_android.crypto

import android.util.Base64
import com.safeguard.encrypt_android.ui.screens.wrapAsPem
import java.security.*
import java.security.spec.*

object KeyUtils {

    fun generateRSAKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    fun encodePrivateKeyToPKCS1Pem(key: PrivateKey): String {
        val pkcs8 = key.encoded
        val pkcs1 = pkcs8.copyOfRange(26, pkcs8.size) // PKCS#8 → PKCS#1
        val base64 = Base64.encodeToString(pkcs1, Base64.NO_WRAP)
        return wrapAsPem(base64, "RSA PRIVATE KEY")
    }

    fun encodePublicKeyToPKCS1Pem(key: PublicKey): String {
        val rsaPublicKey = key as java.security.interfaces.RSAPublicKey
        val modulus = rsaPublicKey.modulus
        val exponent = rsaPublicKey.publicExponent

        // Codificar en ASN.1 DER manualmente (para PKCS#1)
        val modulusBytes = modulus.toByteArray().stripLeadingZero()
        val exponentBytes = exponent.toByteArray().stripLeadingZero()

        val der = buildDerSequence(modulusBytes, exponentBytes)
        val base64 = Base64.encodeToString(der, Base64.NO_WRAP)
        return wrapAsPem(base64, "RSA PUBLIC KEY")
    }

    private fun ByteArray.stripLeadingZero(): ByteArray =
        if (this[0] == 0.toByte()) this.copyOfRange(1, this.size) else this

    private fun buildDerSequence(modulus: ByteArray, exponent: ByteArray): ByteArray {
        fun encodeInteger(bytes: ByteArray): ByteArray {
            val len = bytes.size
            val header = byteArrayOf(0x02, len.toByte())
            return header + bytes
        }

        val modEnc = encodeInteger(modulus)
        val expEnc = encodeInteger(exponent)
        val totalLength = (modEnc.size + expEnc.size).toByte()
        return byteArrayOf(0x30, totalLength) + modEnc + expEnc
    }



    fun decodePublicKeyFromBase64(encoded: String): PublicKey {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    fun decodePrivateKeyFromBase64(encoded: String): PrivateKey {
        val cleaned = encoded
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val pkcs1Bytes = Base64.decode(cleaned, Base64.NO_WRAP)

        // Convertir PKCS#1 a PKCS#8 envolviéndolo manualmente (necesario para Java KeyFactory)
        val pkcs1ToPkcs8 = wrapRsaPkcs1ToPkcs8(pkcs1Bytes)
        val spec = PKCS8EncodedKeySpec(pkcs1ToPkcs8)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    fun wrapRsaPkcs1ToPkcs8(pkcs1: ByteArray): ByteArray {
        val pkcs1Length = pkcs1.size
        val totalLength = pkcs1Length + 26
        val result = ByteArray(totalLength)

        val header = byteArrayOf(
            0x30.toByte(), 0x82.toByte(), ((totalLength - 4) shr 8).toByte(), ((totalLength - 4) and 0xff).toByte(),
            0x02, 0x01, 0x00,
            0x30, 0x0d,
            0x06, 0x09, 0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01,
            0x05, 0x00,
            0x04, 0x82.toByte(), (pkcs1Length shr 8).toByte(), (pkcs1Length and 0xff).toByte()
        )

        System.arraycopy(header, 0, result, 0, header.size)
        System.arraycopy(pkcs1, 0, result, header.size, pkcs1.size)
        return result
    }

    fun convertPrivateKeyToPKCS1(privateKey: PrivateKey): String {
        val encoded = privateKey.encoded

        // Extraer bytes PKCS#1 desde PKCS#8 (salta encabezado DER de 26 bytes)
        val pkcs1Bytes = encoded.copyOfRange(26, encoded.size)

        val base64 = Base64.encodeToString(pkcs1Bytes, Base64.NO_WRAP)
        return """
        -----BEGIN RSA PRIVATE KEY-----
        $base64
        -----END RSA PRIVATE KEY-----
    """.trimIndent()
    }



}
