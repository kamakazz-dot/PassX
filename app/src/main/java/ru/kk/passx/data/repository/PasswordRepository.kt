package ru.kk.passx.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kk.passx.data.crypto.CryptoManager
import ru.kk.passx.data.local.PasswordDao
import ru.kk.passx.data.local.PasswordEntity
import ru.kk.passx.domain.model.Category
import ru.kk.passx.domain.model.PasswordItem

/**
 * Репозиторий для управления записями паролей.
 * Выполняет прозрачное шифрование/расшифрование данных между базой Room и слоем Domain/UI.
 */
interface PasswordRepository {
    fun getAllPasswords(): Flow<List<PasswordItem>>
    fun searchPasswords(query: String): Flow<List<PasswordItem>>
    suspend fun getPasswordById(id: Long): PasswordItem?
    suspend fun savePassword(item: PasswordItem): Long
    suspend fun deletePassword(item: PasswordItem)
    suspend fun deleteAll()
}

class PasswordRepositoryImpl(
    private val passwordDao: PasswordDao,
    private val cryptoManager: CryptoManager
) : PasswordRepository {

    override fun getAllPasswords(): Flow<List<PasswordItem>> {
        return passwordDao.getAllPasswords().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchPasswords(query: String): Flow<List<PasswordItem>> {
        return passwordDao.searchPasswords(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getPasswordById(id: Long): PasswordItem? {
        return passwordDao.getPasswordById(id)?.toDomainModel()
    }

    override suspend fun savePassword(item: PasswordItem): Long {
        val entity = item.toEntity()
        return if (item.id == 0L) {
            passwordDao.insertPassword(entity)
        } else {
            passwordDao.updatePassword(entity)
            item.id
        }
    }

    override suspend fun deletePassword(item: PasswordItem) {
        passwordDao.deletePassword(item.toEntity())
    }

    override suspend fun deleteAll() {
        passwordDao.deleteAll()
    }

    // Вспомогательные функции маппинга
    private fun PasswordEntity.toDomainModel(): PasswordItem {
        val decryptedPassword = try {
            cryptoManager.decrypt(encryptedPassword)
        } catch (e: Exception) {
            "*** Ошибка расшифровки ***"
        }
        return PasswordItem(
            id = id,
            title = title,
            serviceUrl = serviceUrl,
            login = login,
            passwordPlain = decryptedPassword,
            category = Category.fromString(category),
            isFavorite = isFavorite,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PasswordItem.toEntity(): PasswordEntity {
        val encryptedBytes = cryptoManager.encrypt(passwordPlain)
        return PasswordEntity(
            id = id,
            title = title,
            serviceUrl = serviceUrl,
            login = login,
            encryptedPassword = encryptedBytes,
            category = category.name,
            isFavorite = isFavorite,
            notes = notes,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}
