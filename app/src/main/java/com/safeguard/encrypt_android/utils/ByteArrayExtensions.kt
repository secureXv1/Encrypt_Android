// utils/ByteArrayExtensions.kt
package com.safeguard.encrypt_android.utils

fun ByteArray.indexOfFirstSlice(sub: ByteArray): Int {
    outer@ for (i in 0..this.size - sub.size) {
        for (j in sub.indices) {
            if (this[i + j] != sub[j]) continue@outer
        }
        return i
    }
    return -1
}