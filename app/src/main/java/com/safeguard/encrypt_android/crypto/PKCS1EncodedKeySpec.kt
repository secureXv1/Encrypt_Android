package com.safeguard.encrypt_android.crypto


import java.security.spec.KeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

class PKCS1EncodedKeySpec(pkcs1Bytes: ByteArray) : KeySpec {
    val encoded: ByteArray

    init {
        try {
            val pkcs1 = pkcs1Bytes.copyOf()
            val pkcs1Length = pkcs1.size

            val pkcs8Header = byteArrayOf(
                0x30.toByte(), 0x82.toByte(), ((pkcs1Length + 22) shr 8).toByte(), ((pkcs1Length + 22) and 0xff).toByte(),
                0x02, 0x01, 0x00,
                0x30, 0x0d,
                0x06, 0x09,
                0x2a.toByte(), 0x86.toByte(), 0x48.toByte(), 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00,
                0x04, 0x82.toByte(), (pkcs1Length shr 8).toByte(), (pkcs1Length and 0xff).toByte()
            )

            encoded = pkcs8Header + pkcs1
        } catch (e: Exception) {
            throw InvalidKeySpecException("No se pudo convertir PKCS#1 a PKCS#8: ${e.message}")
        }
    }

    fun toPKCS8(): PKCS8EncodedKeySpec = PKCS8EncodedKeySpec(encoded)
}
