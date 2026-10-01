package ru.kk.passx.ui.generator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.kk.passx.domain.generator.PasswordGenerator

data class GeneratorUiState(
    val config: PasswordGenerator.GeneratorConfig = PasswordGenerator.GeneratorConfig(),
    val generatedPassword: String = "",
    val strength: PasswordGenerator.Strength = PasswordGenerator.Strength.MEDIUM
)

class GeneratorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        generatePassword()
    }

    fun updateConfig(config: PasswordGenerator.GeneratorConfig) {
        _uiState.value = _uiState.value.copy(config = config)
        generatePassword()
    }

    fun generatePassword() {
        val config = _uiState.value.config
        val newPassword = PasswordGenerator.generate(config)
        val strength = PasswordGenerator.evaluateStrength(newPassword)
        _uiState.value = _uiState.value.copy(
            generatedPassword = newPassword,
            strength = strength
        )
    }
}
