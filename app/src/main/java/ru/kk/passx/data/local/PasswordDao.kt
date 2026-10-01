package ru.kk.passx.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) — интерфейс с SQL-запросами для работы с таблицей зашифрованных паролей.
 */
@Dao
interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY title ASC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntity?

    @Query("SELECT * FROM passwords WHERE title LIKE '%' || :query || '%' OR login LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entity: PasswordEntity): Long

    @Update
    suspend fun updatePassword(entity: PasswordEntity)

    @Delete
    suspend fun deletePassword(entity: PasswordEntity)

    @Query("DELETE FROM passwords")
    suspend fun deleteAll()
}
