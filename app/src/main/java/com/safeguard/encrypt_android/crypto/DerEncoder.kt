package com.safeguard.encrypt_android.crypto

object DerEncoder {
    fun encodeLength(length: Int): ByteArray {
        return if (length < 128) {
            byteArrayOf(length.toByte())
        } else {
            val bytes = length.toBigInteger().toByteArray().let {
                if (it[0] == 0.toByte()) it.drop(1).toByteArray() else it
            }
            byteArrayOf((0x80 or bytes.size).toByte()) + bytes
        }
    }

    fun encodeSequence(content: ByteArray): ByteArray {
        val length = encodeLength(content.size)
        return byteArrayOf(0x30) + length + content
    }

    fun encodeBitString(content: ByteArray): ByteArray {
        val length = encodeLength(content.size)
        return byteArrayOf(0x03) + length + content
    }
}
