import android.os.Environment
import com.safeguard.encrypt_android.crypto.CryptoUtils
import com.safeguard.encrypt_android.crypto.MasterKey
import com.safeguard.endcrypt_android.MyApp
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object Encryptor {

    enum class Metodo {
        PASSWORD,
        RSA
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    private fun getOutputFilesFrom(inputFile: File): Pair<File, File> {
        // ✅ Obtener nombre base del archivo (sin extensión)
        val originalName = inputFile.nameWithoutExtension
        val fileName = "${originalName}_Cif.json"

        // ✅ Carpeta pública (visible al usuario)
        val publicDir = File(
            MyApp.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Encrypt_Android"
        )

        // ✅ Carpeta privada (oculta al usuario)
        val privateDir = File(
            MyApp.context.filesDir,
            "EncryptApp"
        )

        // Crear carpetas si no existen
        if (!publicDir.exists()) publicDir.mkdirs()
        if (!privateDir.exists()) privateDir.mkdirs()

        // Devolver archivos con rutas correctas
        return Pair(
            File(publicDir, fileName),
            File(privateDir, fileName)
        )
    }



    fun encryptWithPassword(inputFile: File, password: String, userUuid: String): File {
        val saltUser = CryptoUtils.generateRandomBytes(16)
        val saltAdmin = CryptoUtils.generateRandomBytes(16)
        val ivUser = CryptoUtils.generateRandomBytes(16)
        val ivAdmin = CryptoUtils.generateRandomBytes(16)

        val keyUser = CryptoUtils.deriveKeyFromPassword(password, saltUser)
        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)

        // Cifrar contenido con clave del usuario
        val cipherUser = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherUser.init(Cipher.ENCRYPT_MODE, keyUser, IvParameterSpec(ivUser))
        val encryptedContent = cipherUser.doFinal(inputFile.readBytes())

        // Cifrar la contraseña con clave del admin
        val cipherAdmin = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherAdmin.init(Cipher.ENCRYPT_MODE, keyAdmin, IvParameterSpec(ivAdmin))
        val encryptedPassword = cipherAdmin.doFinal(password.toByteArray(Charsets.UTF_8))

        val json = JSONObject().apply {
            put("type", "password")
            put("filename", inputFile.name)
            put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
            put("salt_user", android.util.Base64.encodeToString(saltUser, android.util.Base64.NO_WRAP))
            put("salt_admin", android.util.Base64.encodeToString(saltAdmin, android.util.Base64.NO_WRAP))
            put("iv_user", ivUser.toHexString())
            put("iv_admin", ivAdmin.toHexString())
            put("data", encryptedContent.toHexString())
            put("encrypted_user_password", encryptedPassword.toHexString())
            put("created_by", userUuid)
        }

        val (publicFile, privateFile) = getOutputFilesFrom(inputFile)

        // ✅ Refuerzo de seguridad de escritura
        publicFile.parentFile?.mkdirs()
        privateFile.parentFile?.mkdirs()

        publicFile.writeText(json.toString())
        privateFile.writeText(json.toString())
        saveCopyToDatFolder(publicFile.name, json.toString())

        return publicFile
    }


    fun encryptWithPublicKey(inputFile: File, publicKeyPEM: String, userUuid: String): File {
        val secretKey = CryptoUtils.generateAESKey()
        val iv = CryptoUtils.generateRandomBytes(16)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(inputFile.readBytes())

        val encryptedKeyUser = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, publicKeyPEM)
        val encryptedKeyMaster = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, MasterKey.PUBLIC_KEY_PEM)

        val json = JSONObject().apply {
            put("type", "rsa")
            put("filename", inputFile.name)
            put("ext", inputFile.extension.let { if (it.startsWith(".")) it else ".$it" })
            put("key_user", encryptedKeyUser.toHexString())
            put("key_master", encryptedKeyMaster.toHexString())
            put("iv", iv.toHexString())
            put("data", encrypted.toHexString())
            put("created_by", userUuid)
        }

        val (publicFile, privateFile) = getOutputFilesFrom(inputFile)

        // ✅ Refuerzo: asegúrate que las carpetas existen
        publicFile.parentFile?.mkdirs()
        privateFile.parentFile?.mkdirs()

        publicFile.writeText(json.toString())
        privateFile.writeText(json.toString())
        saveCopyToDatFolder(publicFile.name, json.toString())

        return publicFile
    }

    private fun saveCopyToDatFolder(fileName: String, content: String) {
        val datDir = File(MyApp.context.filesDir, "Encrypt_Android/dat")
        if (!datDir.exists()) datDir.mkdirs()

        val copyFile = File(datDir, fileName)
        copyFile.writeText(content)
    }


}
