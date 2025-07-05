package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.utils.convertImageToPdf
import com.safeguard.encrypt_android.utils.ocultarArchivo
import java.io.File

@Composable
fun FilePickerForHiding(encryptedFile: File, onFinish: () -> Unit) {
    val context = LocalContext.current
    var filePicked by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            filePicked = uri
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("*/*"))
    }

    filePicked?.let { uri ->
        val fileName = getFileNameFromUri(context, uri)
        val mimeType = context.contentResolver.getType(uri)
        val inputStream = context.contentResolver.openInputStream(uri)
        val containerBytes = inputStream?.readBytes()

        if (fileName != null && containerBytes != null) {
            val tempContainer = File(context.cacheDir, fileName)
            tempContainer.writeBytes(containerBytes)

            val processedContainer = if (mimeType != null && isImageFile(mimeType)) {
                convertImageToPdf(context, tempContainer.readBytes(), tempContainer.name)
            } else {
                tempContainer
            }


            val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
            outputDir.mkdirs()

            val outputFile = File(outputDir, processedContainer.nameWithoutExtension + "_oculto." + processedContainer.extension)
            ocultarArchivo(processedContainer, encryptedFile, outputFile)

            val uriFinal = FileProvider.getUriForFile(context, "${context.packageName}.provider", outputFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uriFinal)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir archivo oculto"))

            onFinish()
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(uri, null, null, null, null)
    var fileName: String? = null

    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex != -1) {
            fileName = it.getString(nameIndex)
        }
    }

    return fileName
}

fun isImageFile(mimeType: String): Boolean {
    return mimeType.startsWith("image/")
}
