package com.safeguard.encrypt_android.crypto

import android.os.Environment
import java.io.File

object CryptoController {

    fun encrypt(
        inputFile: File,
        method: Encryptor.Metodo,
        password: String? = null,
        publicKeyPEM: String? = null,
        userUuid: String
    ): File {
        return when (method) {
            Encryptor.Metodo.PASSWORD -> {
                require(!password.isNullOrBlank()) { "Se requiere una contraseña válida." }
                Encryptor.encryptWithPassword(inputFile, password, userUuid)
            }

            Encryptor.Metodo.RSA -> {
                require(!publicKeyPEM.isNullOrBlank()) { "Se requiere una clave pública válida." }
                Encryptor.encryptWithPublicKey(inputFile, publicKeyPEM, userUuid)
            }
        }
    }

    fun decrypt(
        inputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String? = null,
        allowAdminRecovery: Boolean = false
    ): File {
        val (decryptedBytes, outputFileName) = Decryptor.decryptFile(
            inputFile = inputFile,
            promptForPassword = promptForPassword,
            privateKeyPEM = privateKeyPEM,
            allowAdminRecovery = allowAdminRecovery
        )

        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Encrypt_Android"
        )
        if (!outputDir.exists()) outputDir.mkdirs()

        val outputFile = File(outputDir, outputFileName)
        outputFile.writeBytes(decryptedBytes)

        return outputFile
    }
}
