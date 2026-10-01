package ru.kk.passx.ui.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kk.passx.domain.generator.PasswordGenerator
import ru.kk.passx.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Генератор паролей", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Карточка результат
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.generatedPassword,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val (color, text) = when (state.strength) {
                        PasswordGenerator.Strength.WEAK -> Color.Red to "Слабый пароль"
                        PasswordGenerator.Strength.MEDIUM -> Color(0xFFFF9800) to "Средний пароль"
                        PasswordGenerator.Strength.STRONG -> Color(0xFF4CAF50) to "Надежный пароль"
                    }

                    AssistChip(
                        onClick = { },
                        label = { Text(text, color = Color.White, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = color)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.generatePassword() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Обновить")
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Password", state.generatedPassword))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Пароль скопирован")
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Копировать")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Настройки длины
            Text(
                text = "Длина пароля: ${state.config.length}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = state.config.length.toFloat(),
                onValueChange = {
                    viewModel.updateConfig(state.config.copy(length = it.toInt()))
                },
                valueRange = 6f..32f,
                steps = 25
            )

            HorizontalDivider()

            // Настройки символов
            OptionRow(
                title = "Заглавные буквы (A-Z)",
                checked = state.config.useUppercase,
                onCheckedChange = { viewModel.updateConfig(state.config.copy(useUppercase = it)) }
            )

            OptionRow(
                title = "Строчные буквы (a-z)",
                checked = state.config.useLowercase,
                onCheckedChange = { viewModel.updateConfig(state.config.copy(useLowercase = it)) }
            )

            OptionRow(
                title = "Цифры (0-9)",
                checked = state.config.useDigits,
                onCheckedChange = { viewModel.updateConfig(state.config.copy(useDigits = it)) }
            )

            OptionRow(
                title = "Спецсимволы (!@#$)",
                checked = state.config.useSymbols,
                onCheckedChange = { viewModel.updateConfig(state.config.copy(useSymbols = it)) }
            )
        }
    }
}

@Composable
fun OptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
