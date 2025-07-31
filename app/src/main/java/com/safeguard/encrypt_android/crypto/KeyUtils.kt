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




      fun wrapAsPem(base64: String, title: String): String {
        return """
            -----BEGIN $title-----
            $base64
            -----END $title-----
        """.trimIndent()
    }

    fun pemToPkcs8(pem: String): ByteArray {
        val base64 = pem
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val raw = Base64.decode(base64, Base64.DEFAULT)

        return if (pem.contains("RSA PUBLIC KEY")) {
            wrapPkcs1ToPkcs8(raw)
        } else {
            raw
        }
    }

    fun wrapPkcs1ToPkcs8(pkcs1: ByteArray): ByteArray {
        val rsaOid: ByteArray = byteArrayOf(
            0x30, 0x0D,
            0x06, 0x09,
            0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01, // rsaEncryption OID
            0x05, 0x00 // NULL
        )

        val bitString: ByteArray = byteArrayOf(0x00) + pkcs1
        val bitStringLength = encodeLength(bitString.size)
        val bitStringSeq = byteArrayOf(0x03) + bitStringLength + bitString

        val totalSeq = rsaOid + bitStringSeq
        val totalSeqLength = encodeLength(totalSeq.size)

        return byteArrayOf(0x30) + totalSeqLength + totalSeq
    }

    private fun encodeLength(length: Int): ByteArray {
        return if (length < 128) {
            byteArrayOf(length.toByte())
        } else {
            val bytes = mutableListOf<Byte>()
            var temp = length
            while (temp > 0) {
                bytes.add(0, (temp and 0xFF).toByte())
                temp = temp shr 8
            }
            byteArrayOf((0x80 or bytes.size).toByte()) + bytes.toByteArray()
        }
    }





}
