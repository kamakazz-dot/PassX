package ru.kk.passx.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kk.passx.data.repository.PasswordRepository
import ru.kk.passx.data.security.MasterPasswordManager

data class SettingsUiState(
    val message: String? = null,
    val isDataCleared: Boolean = false,
    val isBiometricEnabled: Boolean = true
)

class SettingsViewModel(
    private val masterPasswordManager: MasterPasswordManager,
    private val repository: PasswordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isBiometricEnabled = masterPasswordManager.isBiometricEnabled()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleBiometric(enabled: Boolean) {
        masterPasswordManager.setBiometricEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            isBiometricEnabled = enabled,
            message = if (enabled) "Вход по биометрии включен" else "Вход по биометрии отключен"
        )
    }

    fun changeMasterPassword(oldPass: String, newPass: String, confirmPass: String) {
        if (!masterPasswordManager.verifyMasterPassword(oldPass)) {
            _uiState.value = _uiState.value.copy(message = "Старый мастер-пароль неверный")
            return
        }
        if (newPass.length < 4) {
            _uiState.value = _uiState.value.copy(message = "Новый пароль слишком короткий (мин. 4 символа)")
            return
        }
        if (newPass != confirmPass) {
            _uiState.value = _uiState.value.copy(message = "Новые пароли не совпадают")
            return
        }

        masterPasswordManager.setMasterPassword(newPass)
        _uiState.value = _uiState.value.copy(message = "Мастер-пароль успешно изменен!")
    }

    fun wipeAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAll()
            masterPasswordManager.reset()
            _uiState.value = _uiState.value.copy(
                message = "Все данные и мастер-пароль успешно удалены!",
                isDataCleared = true
            )
            onComplete()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    class Factory(
        private val masterPasswordManager: MasterPasswordManager,
        private val repository: PasswordRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(masterPasswordManager, repository) as T
        }
    }
}
