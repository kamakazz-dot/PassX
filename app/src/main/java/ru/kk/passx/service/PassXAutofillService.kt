package ru.kk.passx.service

import android.app.assist.AssistStructure
import android.content.Context
import android.os.CancellationSignal
import android.os.Build
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.kk.passx.R
import ru.kk.passx.data.crypto.CryptoManager
import ru.kk.passx.data.local.AppDatabase
import ru.kk.passx.data.repository.PasswordRepositoryImpl

/**
 * Системная служба автозаполнения Android (AutofillService).
 * Позволяет подставлять сохраненные логины и пароли в сторонние приложения и браузеры.
 */
@RequiresApi(Build.VERSION_CODES.O)
class PassXAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val parsedFields = parseStructure(structure)
        if (parsedFields.usernameId == null && parsedFields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val packageName = structure.activityComponent?.packageName ?: ""

        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val repository = PasswordRepositoryImpl(db.passwordDao(), CryptoManager())
                val passwords = repository.getAllPasswords().first()

                if (passwords.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val targetQuery = parsedFields.webDomain ?: packageName

                // Фильтруем записи по веб-домену (например vk.com), URL или названию пакета приложения
                val matchedPasswords = passwords.filter {
                    targetQuery.isNotBlank() && (
                            it.serviceUrl.contains(targetQuery, ignoreCase = true) ||
                                    targetQuery.contains(it.serviceUrl, ignoreCase = true) ||
                                    it.title.contains(targetQuery, ignoreCase = true)
                            )
                }.ifEmpty { passwords.take(5) }

                val responseBuilder = FillResponse.Builder()

                for (item in matchedPasswords) {
                    val remoteViews = RemoteViews(applicationContext.packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.text_autofill_title, "PassX: ${item.title}")
                        setTextViewText(R.id.text_autofill_login, item.login)
                    }

                    val datasetBuilder = Dataset.Builder()

                    parsedFields.usernameId?.let { id ->
                        datasetBuilder.setValue(id, AutofillValue.forText(item.login), remoteViews)
                    }
                    parsedFields.passwordId?.let { id ->
                        datasetBuilder.setValue(id, AutofillValue.forText(item.passwordPlain), remoteViews)
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                // Добавляем SaveInfo для перехвата формы и предложения сохранить новые логин/пароль
                val autofillIds = mutableListOf<AutofillId>()
                parsedFields.usernameId?.let { autofillIds.add(it) }
                parsedFields.passwordId?.let { autofillIds.add(it) }

                if (autofillIds.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        autofillIds.toTypedArray()
                    ).build()
                    responseBuilder.setSaveInfo(saveInfo)
                }

                callback.onSuccess(responseBuilder.build())

            } catch (e: Exception) {
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }

        val packageName = structure.activityComponent?.packageName ?: ""
        val extracted = extractSubmittedCredentials(structure)

        val username = extracted.username
        val password = extracted.password
        val domain = extracted.webDomain ?: packageName

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            serviceScope.launch {
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val repository = PasswordRepositoryImpl(db.passwordDao(), CryptoManager())

                    val existingList = repository.getAllPasswords().first()
                    val isDuplicate = existingList.any {
                        it.login == username && (it.serviceUrl.contains(domain, ignoreCase = true) || domain.contains(it.serviceUrl, ignoreCase = true))
                    }

                    if (!isDuplicate) {
                        val cleanTitle = domain
                            .replace("com.", "")
                            .replace("ru.", "")
                            .replace("https://", "")
                            .replace("http://", "")
                            .take(20)
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }

                        val newItem = ru.kk.passx.domain.model.PasswordItem(
                            title = if (cleanTitle.isNotBlank()) cleanTitle else "Автосохранение",
                            serviceUrl = domain,
                            login = username,
                            passwordPlain = password,
                            category = ru.kk.passx.domain.model.Category.GENERAL,
                            notes = "Автоматически сохранено из $domain"
                        )
                        repository.savePassword(newItem)
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки сохранения
                } finally {
                    callback.onSuccess()
                }
            }
        } else {
            callback.onSuccess()
        }
    }

    private data class ExtractedCredentials(
        var username: String? = null,
        var password: String? = null,
        var webDomain: String? = null
    )

    private fun extractSubmittedCredentials(structure: AssistStructure): ExtractedCredentials {
        val extracted = ExtractedCredentials()
        val count = structure.windowNodeCount
        for (i in 0 until count) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            traverseNodeForSave(node, extracted)
        }
        return extracted
    }

    private fun traverseNodeForSave(node: AssistStructure.ViewNode, extracted: ExtractedCredentials) {
        if (extracted.webDomain == null && !node.webDomain.isNullOrBlank()) {
            extracted.webDomain = node.webDomain
        }

        val textValue = node.autofillValue?.textValue?.toString()
        if (!textValue.isNullOrBlank()) {
            val hints = node.autofillHints
            if (hints != null) {
                for (hint in hints) {
                    when {
                        hint.contains(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
                                hint.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true) -> {
                            extracted.username = textValue
                        }
                        hint.contains(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) -> {
                            extracted.password = textValue
                        }
                    }
                }
            }

            val idEntry = node.idEntry?.lowercase() ?: ""
            if (extracted.username == null && (idEntry.contains("user") || idEntry.contains("login") || idEntry.contains("email"))) {
                extracted.username = textValue
            }
            if (extracted.password == null && (node.inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 || idEntry.contains("pass") || idEntry.contains("pwd"))) {
                extracted.password = textValue
            }
        }

        for (i in 0 until node.childCount) {
            traverseNodeForSave(node.getChildAt(i), extracted)
        }
    }

    private data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var webDomain: String? = null
    )

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val parsed = ParsedFields()
        val nodesCount = structure.windowNodeCount
        for (i in 0 until nodesCount) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            traverseNode(node, parsed)
        }
        return parsed
    }

    private fun traverseNode(node: AssistStructure.ViewNode, parsed: ParsedFields) {
        if (parsed.webDomain == null && !node.webDomain.isNullOrBlank()) {
            parsed.webDomain = node.webDomain
        }

        val hints = node.autofillHints
        if (hints != null) {
            for (hint in hints) {
                when {
                    hint.contains(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
                            hint.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true) -> {
                        parsed.usernameId = node.autofillId
                    }
                    hint.contains(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) -> {
                        parsed.passwordId = node.autofillId
                    }
                }
            }
        }

        // Резервный анализ по типу и идентификаторам полей
        val idEntry = node.idEntry?.lowercase() ?: ""
        val classSimpleName = node.className?.lowercase() ?: ""

        if (parsed.usernameId == null && (idEntry.contains("user") || idEntry.contains("login") || idEntry.contains("email"))) {
            parsed.usernameId = node.autofillId
        }
        if (parsed.passwordId == null && (idEntry.contains("pass") || idEntry.contains("pwd") || classSimpleName.contains("edittext"))) {
            if (node.inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 || idEntry.contains("pass")) {
                parsed.passwordId = node.autofillId
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), parsed)
        }
    }
}
