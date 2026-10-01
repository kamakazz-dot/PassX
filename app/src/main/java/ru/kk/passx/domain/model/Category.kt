package ru.kk.passx.domain.model

/**
 * Категории для группировки сохраненных паролей.
 */
enum class Category(val displayName: String) {
    GENERAL("Общие"),
    SOCIAL("Соцсети"),
    WORK("Работа"),
    FINANCE("Финансы"),
    ENTERTAINMENT("Развлечения"),
    OTHER("Другое");

    companion object {
        fun fromString(name: String): Category {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: GENERAL
        }
    }
}
