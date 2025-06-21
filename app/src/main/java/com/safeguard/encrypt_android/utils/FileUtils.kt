package com.safeguard.encrypt_android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.database.Cursor
import android.provider.OpenableColumns
import android.widget.Toast


fun openOutputFolder(context: Context, folder: File) {
    Toast.makeText(
        context,
        "📁 Archivo guardado en:\n${folder.absolutePath}",
        Toast.LENGTH_LONG
    ).show()
}



fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = it.getString(index)
        }
    }
    return name
}
