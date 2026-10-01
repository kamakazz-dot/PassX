package ru.kk.passx.domain.model

/**
 * Доменная модель учетной записи (хранит расшифрованный пароль в оперативной памяти).
 */
data class PasswordItem(
    val id: Long = 0,
    val title: String,
    val serviceUrl: String = "",
    val login: String,
    val passwordPlain: String,
    val category: Category = Category.GENERAL,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
