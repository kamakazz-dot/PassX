package ru.kk.passx.domain.model

/**
 * Варианты сортировки списка паролей.
 */
enum class SortOrder(val displayName: String) {
    BY_TITLE_ASC("По названию (А-Я)"),
    BY_TITLE_DESC("По названию (Я-А)"),
    BY_DATE_DESC("Сначала новые"),
    BY_DATE_ASC("Сначала старые")
}
