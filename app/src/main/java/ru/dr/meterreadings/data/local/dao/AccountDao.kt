package ru.dr.meterreadings.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.dr.meterreadings.data.local.entities.AccountEntity

/**
 * DAO для работы с таблицей accounts
 *
 * Room автоматически генерирует реализацию этого интерфейса
 */
@Dao
interface AccountDao {

    /**
     * Вставить новый account
     * REPLACE - если account с таким id уже есть, заменить его
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    /**
     * Вставить несколько accounts за одну транзакцию
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    /**
     * Обновить существующий account
     */
    @Update
    suspend fun update(account: AccountEntity)

    /**
     * Удалить account
     */
    @Delete
    suspend fun delete(account: AccountEntity)

    /**
     * Удалить account по ID
     */
    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: String)

    /**
     * Получить account по ID
     * Flow - автоматически обновляется при изменениях в БД
     */
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getById(accountId: String): Flow<AccountEntity?>

    /**
     * Получить все accounts конкретного профиля
     * Flow - UI будет автоматически обновляться
     */
    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getByProfileId(profileId: String): Flow<List<AccountEntity>>

    /**
     * Получить все accounts конкретной компании (Provider)
     */
    @Query("SELECT * FROM accounts WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getByProviderId(providerId: String): Flow<List<AccountEntity>>

    /**
     * Получить все accounts
     */
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AccountEntity>>

    /**
     * Проверить, существует ли account с таким номером у данного профиля
     * Используется при добавлении нового account (избежать дублей)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM accounts WHERE profileId = :profileId AND accountNumber = :accountNumber)")
    suspend fun exists(profileId: String, accountNumber: String): Boolean

    /**
     * Получить количество accounts у профиля
     */
    @Query("SELECT COUNT(*) FROM accounts WHERE profileId = :profileId")
    suspend fun getCountByProfileId(profileId: String): Int

    /**
     * Удалить все accounts профиля
     * (хотя CASCADE в ForeignKey уже делает это автоматически)
     */
    @Query("DELETE FROM accounts WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: String)
}
