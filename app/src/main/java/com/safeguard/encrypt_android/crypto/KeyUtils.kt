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
}
