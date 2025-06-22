package com.safeguard.encrypt_android.crypto

import android.os.Environment
import java.io.File

object CryptoController {

    /**
     * Cifra un archivo usando el método seleccionado.
     * El archivo cifrado se guarda en Download/Encrypt_Android/nombre_cif.json
     * Retorna el archivo generado.
     */
    fun encrypt(
        inputFile: File,
        method: Encryptor.Metodo,
        password: String? = null,
        publicKeyPEM: String? = null
    ): File {
        return when (method) {
            Encryptor.Metodo.PASSWORD -> {
                require(!password.isNullOrBlank()) { "Se requiere una contraseña válida." }
                Encryptor.encryptWithPassword(inputFile, password)
            }

            Encryptor.Metodo.RSA -> {
                require(!publicKeyPEM.isNullOrBlank()) { "Se requiere una clave pública válida." }
                Encryptor.encryptWithPublicKey(inputFile, publicKeyPEM)
            }
        }
    }

    /**
     * Descifra un archivo .json y lo guarda en Download/Encrypt_Android/nombre_dec.ext
     * Retorna el archivo generado.
     */
    fun decrypt(
        inputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String? = null
    ): File {
        val (decryptedBytes, outputFileName) = Decryptor.decryptFile(
            inputFile = inputFile,
            promptForPassword = promptForPassword,
            privateKeyPEM = privateKeyPEM
        )

        val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
        if (!outputDir.exists()) outputDir.mkdirs()

        val outputFile = File(outputDir, outputFileName)
        outputFile.writeBytes(decryptedBytes)

        return outputFile
    }
}
