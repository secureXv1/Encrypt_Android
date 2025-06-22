package com.safeguard.encrypt_android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.database.Cursor
import android.provider.OpenableColumns
import android.widget.Toast
import android.os.Environment
import com.safeguard.encrypt_android.crypto.Decryptor


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

fun handleDecryptionAndOpen(
    context: Context,
    encryptedFile: File,
    promptForPassword: () -> String,
    privateKeyPEM: String? = null
) {
    try {
        val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
        if (!outputDir.exists()) outputDir.mkdirs()

        val (decryptedBytes, fileName) = Decryptor.decryptFile(
            inputFile = encryptedFile,
            promptForPassword = promptForPassword,
            privateKeyPEM = privateKeyPEM
        )

        val finalFile = File(outputDir, fileName)
        finalFile.writeBytes(decryptedBytes)

        Toast.makeText(context, "✅ Guardado en:\n${finalFile.absolutePath}", Toast.LENGTH_LONG).show()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            finalFile
        )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "*/*")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK

        context.startActivity(intent)

    } catch (e: Exception) {
        Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
