package ru.kk.passx.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object VaultList : Screen("vault_list")
    object AddEditPassword : Screen("add_edit_password?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "add_edit_password?id=$id" else "add_edit_password"
    }
    object Generator : Screen("generator")
    object Settings : Screen("settings")
}
