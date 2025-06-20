package com.safeguard.encrypt_android.utils

import android.content.Context
import java.util.UUID

object UuidUtils {
    private const val PREFS_NAME = "securex_prefs"
    private const val UUID_KEY = "uuid"

    fun getClientUUID(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(UUID_KEY, null)
        if (existing != null) return existing

        val newUuid = UUID.randomUUID().toString()
        prefs.edit().putString(UUID_KEY, newUuid).apply()
        return newUuid
    }
}
