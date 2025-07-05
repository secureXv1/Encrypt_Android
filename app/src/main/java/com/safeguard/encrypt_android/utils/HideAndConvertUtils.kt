
package com.safeguard.encrypt_android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun getFileNameWithExtensionFromUri(context: Context, uri: Uri): String {
    val contentResolver = context.contentResolver
    var name: String? = null
    var extension: String? = null

    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) {
            name = it.getString(nameIndex)
        }
    }

    if (name != null && !name.contains(".")) {
        val type = contentResolver.getType(uri)
        extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type ?: "")
        if (extension != null) {
            name += ".$extension"
        }
    }

    return name ?: "archivo"
}

fun isImageFile(fileName: String): Boolean {
    val ext = fileName.lowercase()
    return ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png") || ext.endsWith(".bmp") || ext.endsWith(".webp")
}

fun convertImageToPdf(context: Context, imageBytes: ByteArray, imageName: String): File {
    val pdfFileName = imageName.substringBeforeLast(".") + ".pdf"
    val outputDir = File(context.filesDir, "TempPDF").apply { mkdirs() }
    val pdfFile = File(outputDir, pdfFileName)

    val outputStream = FileOutputStream(pdfFile)
    outputStream.write(imageBytes) // Aquí debería ir la lógica real para convertir a PDF
    outputStream.close()

    return pdfFile
}

fun hideEncryptedFileInContainer(encryptedBytes: ByteArray, containerBytes: ByteArray, outputFileName: String): File {
    val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
    outputDir.mkdirs()

    val outputFile = File(outputDir, outputFileName)
    val delimiter = "<<--BETTY_START-->>".toByteArray()
    outputFile.writeBytes(containerBytes + delimiter + encryptedBytes)

    return outputFile
}

fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
}

fun ocultarArchivo(containerFile: File, encryptedFile: File, outputFile: File) {
    val delimiter = "<<--BETTY_START-->>".toByteArray()
    val contBytes = containerFile.readBytes()
    val cifBytes = encryptedFile.readBytes()
    outputFile.writeBytes(contBytes + delimiter + cifBytes)
}