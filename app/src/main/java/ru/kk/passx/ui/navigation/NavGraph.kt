package ru.kk.passx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.kk.passx.data.repository.PasswordRepository
import ru.kk.passx.data.security.MasterPasswordManager
import ru.kk.passx.ui.auth.AuthScreen
import ru.kk.passx.ui.auth.AuthViewModel
import ru.kk.passx.ui.generator.GeneratorScreen
import ru.kk.passx.ui.generator.GeneratorViewModel
import ru.kk.passx.ui.settings.SettingsScreen
import ru.kk.passx.ui.settings.SettingsViewModel
import ru.kk.passx.ui.vault.AddEditPasswordScreen
import ru.kk.passx.ui.vault.VaultListScreen
import ru.kk.passx.ui.vault.VaultViewModel

@Composable
fun PassXNavGraph(
    navController: NavHostController,
    masterPasswordManager: MasterPasswordManager,
    passwordRepository: PasswordRepository
) {
    val startDestination = if (masterPasswordManager.isUnlocked) {
        Screen.VaultList.route
    } else {
        Screen.Auth.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(masterPasswordManager)
            )
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.VaultList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VaultList.route) {
            val vaultViewModel: VaultViewModel = viewModel(
                factory = VaultViewModel.Factory(passwordRepository)
            )
            VaultListScreen(
                viewModel = vaultViewModel,
                onNavigateToAddEdit = { id ->
                    navController.navigate(Screen.AddEditPassword.createRoute(id))
                },
                onNavigateToGenerator = {
                    navController.navigate(Screen.Generator.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.AddEditPassword.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("id") ?: 0L
            val vaultViewModel: VaultViewModel = viewModel(
                factory = VaultViewModel.Factory(passwordRepository)
            )
            AddEditPasswordScreen(
                itemId = itemId,
                viewModel = vaultViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Generator.route) {
            val generatorViewModel: GeneratorViewModel = viewModel()
            GeneratorScreen(
                viewModel = generatorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(masterPasswordManager, passwordRepository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onDataCleared = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
