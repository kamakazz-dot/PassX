package ru.kk.passx.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Криптографический менеджер на базе Android KeyStore и алгоритма AES-256-GCM.
 * Используется для шифрования и расшифрования паролей перед сохранением в базу данных.
 *
 * Принцип работы:
 * 1. Ключ шифрования создается и хранится в аппаратном модуле Android KeyStore (TEE / StrongBox).
 * 2. Алгоритм AES/GCM/NoPadding обеспечивает не только конфиденциальность, но и целостность данных.
 * 3. При шифровании генерируется случайный вектор инициализации (IV - Initialization Vector).
 * 4. Результат шифрования = (IV + Зашифрованные данные).
 */
class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createSecretKey()
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Зашифровывает строку текста в массив байт (IV + зашифрованный текст).
     */
    fun encrypt(plainText: String): ByteArray {
        if (plainText.isEmpty()) return ByteArray(0)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Объединяем IV (12 байт) и зашифрованные данные в один массив байт
        val result = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)
        return result
    }

    /**
     * Расшифровывает массив байт обратно в исходную строку текста.
     */
    fun decrypt(encryptedBytes: ByteArray): String {
        if (encryptedBytes.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Разделяем IV (первые 12 байт) и зашифрованное тело
        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherText = ByteArray(encryptedBytes.size - GCM_IV_LENGTH)
        
        System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val KEY_ALIAS = "passx_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12 // Длина вектора инициализации для GCM в байтах
        private const val GCM_TAG_LENGTH = 128 // Длина аутентификационного тега в битах
    }
}
