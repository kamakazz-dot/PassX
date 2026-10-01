package ru.kk.passx.ui.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.kk.passx.domain.generator.PasswordGenerator
import ru.kk.passx.domain.model.Category
import ru.kk.passx.domain.model.PasswordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPasswordScreen(
    itemId: Long?,
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val existingItem = remember(itemId, state.passwords) {
        if (itemId != null && itemId != 0L) {
            state.passwords.find { it.id == itemId }
        } else null
    }

    var title by remember { mutableStateOf(existingItem?.title ?: "") }
    var serviceUrl by remember { mutableStateOf(existingItem?.serviceUrl ?: "") }
    var login by remember { mutableStateOf(existingItem?.login ?: "") }
    var password by remember { mutableStateOf(existingItem?.passwordPlain ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: Category.GENERAL) }
    var notes by remember { mutableStateOf(existingItem?.notes ?: "") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(existingItem) {
        existingItem?.let {
            title = it.title
            serviceUrl = it.serviceUrl
            login = it.login
            password = it.passwordPlain
            category = it.category
            notes = it.notes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingItem != null) "Редактирование" else "Новая запись",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                label = { Text("Название (например: VK, Google)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = serviceUrl,
                onValueChange = { serviceUrl = it },
                label = { Text("URL / Приложение (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = login,
                onValueChange = { login = it; errorMessage = null },
                label = { Text("Логин / Email / Телефон") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            password = PasswordGenerator.generate(PasswordGenerator.GeneratorConfig(length = 16))
                        }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Сгенерировать")
                        }
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Показать пароль"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Выбор категории
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    Category.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = {
                                category = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки (необязательно)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Введите название записи"
                        return@Button
                    }
                    if (login.isBlank()) {
                        errorMessage = "Введите логин"
                        return@Button
                    }
                    if (password.isBlank()) {
                        errorMessage = "Введите пароль"
                        return@Button
                    }

                    val newItem = PasswordItem(
                        id = existingItem?.id ?: 0L,
                        title = title.trim(),
                        serviceUrl = serviceUrl.trim(),
                        login = login.trim(),
                        passwordPlain = password,
                        category = category,
                        notes = notes.trim(),
                        createdAt = existingItem?.createdAt ?: System.currentTimeMillis()
                    )

                    viewModel.savePassword(newItem) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (existingItem != null) "Сохранить изменения" else "Добавить в хранилище")
            }
        }
    }
}
