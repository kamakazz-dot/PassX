package ru.kk.passx.data.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Менеджер управления Мастер-паролем приложения и состоянием разблокировки.
 *
 * Принцип безопасности:
 * 1. Сам мастер-пароль НИКОГДА не сохраняется на устройстве.
 * 2. При первом запуске создается случайная "соль" (Salt, 16 байт).
 * 3. Вычисляется хеш = SHA-256(Пароль + Соль). Хеш и соль сохраняются в настройках.
 * 4. При входе введенный пароль проверяется повторным вычислением хеша с сохраненной солью.
 */
class MasterPasswordManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isUnlocked: Boolean = false
        private set

    /**
     * Проверяет, установлен ли уже мастер-пароль для приложения.
     */
    fun isMasterPasswordSet(): Boolean {
        return prefs.contains(KEY_PASSWORD_HASH) && prefs.contains(KEY_SALT)
    }

    /**
     * Устанавливает новый мастер-пароль.
     */
    fun setMasterPassword(password: String): Boolean {
        if (password.length < 4) return false

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()

        isUnlocked = true
        return true
    }

    /**
     * Проверяет введенный мастер-пароль.
     */
    fun verifyMasterPassword(password: String): Boolean {
        if (!isMasterPasswordSet()) return false

        val saltString = prefs.getString(KEY_SALT, null) ?: return false
        val savedHashString = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false

        val salt = Base64.decode(saltString, Base64.NO_WRAP)
        val savedHash = Base64.decode(savedHashString, Base64.NO_WRAP)

        val inputHash = hashPassword(password, salt)

        val isCorrect = inputHash.contentEquals(savedHash)
        if (isCorrect) {
            isUnlocked = true
        }
        return isCorrect
    }

    /**
     * Проверяет, включен ли вход по биометрии.
     */
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    /**
     * Разблокирует хранилище при успешной биометрической аутентификации.
     */
    fun unlockWithBiometric() {
        isUnlocked = true
    }

    /**
     * Блокирует приложение (например, при сворачивании).
     */
    fun lock() {
        isUnlocked = false
    }

    /**
     * Полный сброс мастер-пароля (очистка всех сохраненных ключей).
     */
    fun reset() {
        prefs.edit().clear().apply()
        isUnlocked = false
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val PREFS_NAME = "passx_auth_prefs"
        private const val KEY_SALT = "auth_salt"
        private const val KEY_PASSWORD_HASH = "auth_password_hash"
        private const val KEY_BIOMETRIC_ENABLED = "auth_biometric_enabled"
    }
}
