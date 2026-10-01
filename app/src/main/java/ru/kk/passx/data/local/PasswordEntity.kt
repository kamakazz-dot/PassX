package ru.kk.passx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Таблица "passwords" в локальной базе данных Room.
 * ВАЖНО: Сам пароль зашифрован с помощью AES-GCM (CryptoManager) и хранится в виде массива байт (encryptedPassword).
 */
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val serviceUrl: String,
    val login: String,
    val encryptedPassword: ByteArray,
    val category: String,
    val isFavorite: Boolean = false,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (serviceUrl != other.serviceUrl) return false
        if (login != other.login) return false
        if (!encryptedPassword.contentEquals(other.encryptedPassword)) return false
        if (category != other.category) return false
        if (notes != other.notes) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + serviceUrl.hashCode()
        result = 31 * result + login.hashCode()
        result = 31 * result + encryptedPassword.contentHashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
