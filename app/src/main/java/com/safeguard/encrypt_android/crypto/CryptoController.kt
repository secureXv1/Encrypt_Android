// crypto/CryptoController.kt
package com.safeguard.encrypt_android.crypto

import java.io.File

object CryptoController {

    /**
     * Cifra un archivo usando el método seleccionado (contraseña o clave pública).
     */
    fun encrypt(
        inputFile: File,
        method: Encryptor.Metodo,
        outputFile: File,
        password: String? = null,
        publicKeyPEM: String? = null
    ) {
        when (method) {
            Encryptor.Metodo.PASSWORD -> {
                require(!password.isNullOrBlank()) { "Se requiere una contraseña válida." }
                Encryptor.encryptWithPassword(inputFile, password, outputFile)
            }

            Encryptor.Metodo.RSA -> {
                require(!publicKeyPEM.isNullOrBlank()) { "Se requiere una clave pública válida." }
                Encryptor.encryptWithPublicKey(inputFile, publicKeyPEM, outputFile)
            }
        }
    }

    /**
     * Descifra un archivo cifrado (.json), usando contraseña o clave privada.
     */
    fun decrypt(
        inputFile: File,
        outputFile: File,
        promptForPassword: () -> String,
        privateKeyPEM: String? = null
    ) {
        val (decryptedBytes, extension) = Decryptor.decryptFile(
            inputFile = inputFile,
            promptForPassword = promptForPassword,
            privateKeyPEM = privateKeyPEM
        )

        val finalFile = if (!outputFile.name.endsWith(extension)) {
            File(outputFile.absolutePath + extension)
        } else {
            outputFile
        }

        finalFile.writeBytes(decryptedBytes)
    }
}
