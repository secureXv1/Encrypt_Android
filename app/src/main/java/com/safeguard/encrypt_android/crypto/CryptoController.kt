// crypto/CryptoController.kt
package com.safeguard.encrypt_android.crypto

import java.io.File

object CryptoController {

    enum class EncryptionMethod {
        PASSWORD,
        PUBLIC_KEY
    }

    /**
     * Cifra un archivo según el método elegido.
     *
     * @param inputFile Archivo original
     * @param method Método elegido (contraseña o clave pública)
     * @param outputFile Archivo JSON resultante
     * @param password Contraseña (si aplica)
     * @param publicKeyPEM Contenido PEM (si aplica)
     */
    fun encrypt(
        inputFile: File,
        method: EncryptionMethod,
        outputFile: File,
        password: String? = null,
        publicKeyPEM: String? = null
    ) {
        when (method) {
            EncryptionMethod.PASSWORD -> {
                require(!password.isNullOrBlank()) { "Se requiere una contraseña válida." }
                Encryptor.encryptWithPassword(inputFile, password, outputFile)
            }

            EncryptionMethod.PUBLIC_KEY -> {
                require(!publicKeyPEM.isNullOrBlank()) { "Se requiere una clave pública válida." }
                Encryptor.encryptWithPublicKey(inputFile, publicKeyPEM, outputFile)
            }
        }
    }

    /**
     * Descifra un archivo cifrado (.json)
     *
     * @param inputFile Archivo .json cifrado
     * @param promptForPassword Función que retorna una contraseña (mostrada en un diálogo)
     * @param privateKeyPEM Clave privada si fue cifrado con llave
     * @param outputFile Archivo de salida final
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
