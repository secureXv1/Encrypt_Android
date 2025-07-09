import android.os.Environment
import com.safeguard.encrypt_android.crypto.CryptoUtils
import com.safeguard.encrypt_android.crypto.MasterKey
import com.safeguard.endcrypt_android.MyApp
import org.json.JSONObject
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64


object Encryptor {

    enum class Metodo {
        PASSWORD,
        RSA
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    private fun getOutputFile(inputFile: File): Pair<File, String> {
        val originalName = inputFile.nameWithoutExtension
        val fileName = "${originalName}_Cif.json"

        // ✅ Carpeta privada
        val privateDir = File(MyApp.context.filesDir, "EncryptApp")
        if (!privateDir.exists()) privateDir.mkdirs()

        return Pair(File(privateDir, fileName), fileName)
    }

    private fun saveCopyToDatFolder(fileName: String, content: String) {
        val datDir = File(MyApp.context.filesDir, "Encrypt_Android/dat")
        if (!datDir.exists()) datDir.mkdirs()

        val copyFile = File(datDir, fileName)
        copyFile.writeText(content)
    }





    fun encryptWithPassword(inputFile: File, password: String, userUuid: String): File {
        // 1. Salts e IVs (16 bytes para CBC)
        val saltUser = CryptoUtils.generateRandomBytes(16)
        val saltAdmin = CryptoUtils.generateRandomBytes(16)
        val ivUser = CryptoUtils.generateRandomBytes(16)
        val ivAdmin = CryptoUtils.generateRandomBytes(12) // GCM requiere 12 bytes

        // 2. Derivación de claves
        val keyUser = CryptoUtils.deriveKeyFromPassword(password, saltUser) // 32 bytes
        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)

        // 3. Cifrado con AES-CBC (igual que Windows)
        val cipherUser = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherUser.init(Cipher.ENCRYPT_MODE, keyUser, IvParameterSpec(ivUser))
        val encryptedContent = cipherUser.doFinal(inputFile.readBytes())

        // 4. Cifrado de la contraseña con clave admin (AES-GCM)
        val cipherAdmin = Cipher.getInstance("AES/GCM/NoPadding")
        cipherAdmin.init(Cipher.ENCRYPT_MODE, keyAdmin, GCMParameterSpec(128, ivAdmin))
        val sealed = cipherAdmin.doFinal(password.toByteArray(Charsets.UTF_8))

        // 5. Estructura JSON final
        val json = JSONObject().apply {
            put("type", "password")
            put("filename", inputFile.nameWithoutExtension)
            put("ext", inputFile.extension)
            put("created_by", userUuid)

            put("salt_user", saltUser.toHexString())
            put("salt_admin", saltAdmin.toHexString())
            put("iv_user", ivUser.toHexString())
            put("iv_admin", ivAdmin.toHexString())
            put("data", encryptedContent.toHexString())
            put("encrypted_user_password", sealed.toHexString())
        }

        // 6. Guardar el archivo cifrado
        val (outputFile, fileName) = getOutputFile(inputFile)
        outputFile.writeText(json.toString())
        saveCopyToDatFolder(fileName, json.toString())
        return outputFile
    }



    fun encryptWithPublicKey(inputFile: File, publicKeyPEM: String, userUuid: String): File {
        val secretKey = CryptoUtils.generateAESKey()
        val iv = CryptoUtils.generateRandomBytes(12)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(inputFile.readBytes())

        val encryptedKeyUser = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, publicKeyPEM)
        val encryptedKeyMaster = CryptoUtils.encryptKeyWithPublicKey(secretKey.encoded, MasterKey.PUBLIC_KEY_PEM)

        val json = JSONObject().apply {
            put("type", "rsa")
            put("filename", inputFile.nameWithoutExtension)
            put("ext", inputFile.extension)
            put("created_by", userUuid)

            put("key_user", encryptedKeyUser.toHexString())
            put("key_master", encryptedKeyMaster.toHexString())
            put("iv", iv.toHexString())
            put("data", encrypted.toHexString())
        }

        val (outputFile, fileName) = getOutputFile(inputFile)
        outputFile.writeText(json.toString())
        saveCopyToDatFolder(fileName, json.toString())
        return outputFile
    }



}
