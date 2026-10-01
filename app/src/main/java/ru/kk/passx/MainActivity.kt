package ru.kk.passx

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import ru.kk.passx.data.crypto.CryptoManager
import ru.kk.passx.data.local.AppDatabase
import ru.kk.passx.data.repository.PasswordRepositoryImpl
import ru.kk.passx.data.security.MasterPasswordManager
import ru.kk.passx.ui.navigation.PassXNavGraph
import ru.kk.passx.ui.theme.PassXTheme

class MainActivity : FragmentActivity() {

    private lateinit var masterPasswordManager: MasterPasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Защита от скриншотов и отображения содержимого экрана в списке запущенных приложений
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        // Инициализация компонентов архитектуры
        val cryptoManager = CryptoManager()
        val database = AppDatabase.getInstance(applicationContext)
        val passwordRepository = PasswordRepositoryImpl(database.passwordDao(), cryptoManager)
        masterPasswordManager = MasterPasswordManager(applicationContext)

        setContent {
            PassXTheme {
                val navController = rememberNavController()
                PassXNavGraph(
                    navController = navController,
                    masterPasswordManager = masterPasswordManager,
                    passwordRepository = passwordRepository
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Блокируем приложение при уходе в фоновый режим
        masterPasswordManager.lock()
    }
}
