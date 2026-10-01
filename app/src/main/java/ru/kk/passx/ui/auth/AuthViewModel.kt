package ru.kk.passx.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.kk.passx.data.security.MasterPasswordManager

data class AuthUiState(
    val isMasterPasswordSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val masterPasswordManager: MasterPasswordManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isMasterPasswordSet = masterPasswordManager.isMasterPasswordSet(),
            isUnlocked = masterPasswordManager.isUnlocked
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun createMasterPassword(password: String, confirm: String): Boolean {
        if (password.length < 4) {
            _uiState.value = _uiState.value.copy(errorMessage = "Пароль должен быть не менее 4 символов")
            return false
        }
        if (password != confirm) {
            _uiState.value = _uiState.value.copy(errorMessage = "Пароли не совпадают")
            return false
        }

        val success = masterPasswordManager.setMasterPassword(password)
        if (success) {
            _uiState.value = AuthUiState(
                isMasterPasswordSet = true,
                isUnlocked = true,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Не удалось сохранить мастер-пароль")
        }
        return success
    }

    fun verifyMasterPassword(password: String): Boolean {
        val success = masterPasswordManager.verifyMasterPassword(password)
        if (success) {
            _uiState.value = _uiState.value.copy(
                isUnlocked = true,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Неверный мастер-пароль")
        }
        return success
    }

    fun unlockWithBiometric() {
        masterPasswordManager.unlockWithBiometric()
        _uiState.value = _uiState.value.copy(
            isUnlocked = true,
            errorMessage = null
        )
    }

    fun isBiometricEnabled(): Boolean {
        return masterPasswordManager.isBiometricEnabled()
    }

    fun onBiometricError(error: String) {
        _uiState.value = _uiState.value.copy(errorMessage = error)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun lockApp() {
        masterPasswordManager.lock()
        _uiState.value = _uiState.value.copy(isUnlocked = false)
    }

    class Factory(private val masterPasswordManager: MasterPasswordManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(masterPasswordManager) as T
        }
    }
}
