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
        // 🔹1. Empaquetar archivo como JSON base64
        val fileBytes = inputFile.readBytes()
        val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        val payload = JSONObject().apply {
            put("filename", inputFile.name)
            put("ext", ".${inputFile.extension}")
            put("content", base64Content)
        }
        val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

        // 🔹2. Generar salt e IVs
        val saltUser = CryptoUtils.generateRandomBytes(16)
        val saltAdmin = CryptoUtils.generateRandomBytes(16)
        val ivUser = CryptoUtils.generateRandomBytes(12)  // GCM usa 12 bytes
        val ivAdmin = CryptoUtils.generateRandomBytes(16) // CBC usa 16 bytes

        // 🔹3. Derivar claves
        val keyUser = CryptoUtils.deriveKeyFromPassword(password, saltUser)
        val keyAdmin = CryptoUtils.deriveKeyFromPassword("SeguraAdmin123!", saltAdmin)

        // 🔹4. Cifrar archivo con AES-GCM
        val cipherGCM = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, ivUser)
        cipherGCM.init(Cipher.ENCRYPT_MODE, keyUser, gcmSpec)
        val cipherText = cipherGCM.doFinal(payloadBytes) // incluye tag al final

        // 🔹5. Cifrar contraseña con AES-CBC para administrador
        val cipherAdmin = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherAdmin.init(Cipher.ENCRYPT_MODE, keyAdmin, IvParameterSpec(ivAdmin))
        val encryptedPassword = cipherAdmin.doFinal(password.toByteArray(Charsets.UTF_8))

        // 🔹6. Construir JSON final
        val json = JSONObject().apply {
            put("type", "password")
            put("filename", inputFile.nameWithoutExtension)
            put("ext", ".${inputFile.extension}")
            put("created_by", userUuid)

            put("salt_user", saltUser.toHexString())
            put("salt_admin", saltAdmin.toHexString())
            put("iv_user", ivUser.toHexString())
            put("iv_admin", ivAdmin.toHexString())
            put("data", cipherText.toHexString())
            put("encrypted_user_password", encryptedPassword.toHexString())
        }

        // 🔹7. Guardar archivo cifrado
        val (outputFile, fileName) = getOutputFile(inputFile)
        outputFile.writeText(json.toString())
        saveCopyToDatFolder(fileName, json.toString())
        return outputFile
    }




    fun encryptWithPublicKey(inputFile: File, publicKeyPEM: String, userUuid: String): File {
        // 🔹1. Empaquetar archivo como JSON base64
        val fileBytes = inputFile.readBytes()
        val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        val payload = JSONObject().apply {
            put("filename", inputFile.name)
            put("ext", ".${inputFile.extension}")
            put("content", base64Content)
        }
        val payloadBytes = payload.toString().toByteArray(Charsets.UTF_8)

        // 🔹2. Generar AES key e IV
        val aesKey = CryptoUtils.generateAESKey()
        val aesKeyBytes = aesKey.encoded
        val iv = CryptoUtils.generateRandomBytes(12)  // ✅ GCM requiere 12 bytes

        // 🔹3. Cifrado con AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        val cipherText = cipher.doFinal(payloadBytes) // ciphertext + tag al final

        // 🔹4. Cifrado RSA de la clave AES
        val encryptedKeyUser = CryptoUtils.encryptKeyWithPublicKey(aesKeyBytes, publicKeyPEM)
        val encryptedKeyMaster = CryptoUtils.encryptKeyWithPublicKey(aesKeyBytes, MasterKey.PUBLIC_KEY_PEM)

        // 🔹5. JSON resultante
        val json = JSONObject().apply {
            put("type", "rsa")
            put("filename", inputFile.nameWithoutExtension)
            put("ext", ".${inputFile.extension}")
            put("created_by", userUuid)

            put("key_user", encryptedKeyUser.toHexString())
            put("key_master", encryptedKeyMaster.toHexString())
            put("iv", iv.toHexString())
            put("data", cipherText.toHexString())
        }

        // 🔹6. Guardar archivo
        val (outputFile, fileName) = getOutputFile(inputFile)
        outputFile.writeText(json.toString())
        saveCopyToDatFolder(fileName, json.toString())
        return outputFile
    }





}
