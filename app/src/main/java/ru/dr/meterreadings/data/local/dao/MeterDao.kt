// app/src/main/java/ru/dr/meterreadings/data/local/dao/MeterDao.kt

package ru.dr.meterreadings.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dr.meterreadings.data.local.entities.MeterEntity

@Dao
interface MeterDao {

    /**
     * Получить все счётчики для аккаунта
     */
    @Query("SELECT * FROM meters WHERE accountId = :accountId ORDER BY apiCounterId ASC")
    fun getAllByAccountId(accountId: String): Flow<List<MeterEntity>>

    /**
     * Вставить список счётчиков (заменить при конфликте)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meters: List<MeterEntity>)

    /**
     * Обновить дату последней передачи для счётчика
     */
    @Query("UPDATE meters SET lastSubmissionDate = :date WHERE id = :meterId")
    suspend fun updateSubmissionDate(meterId: String, date: String)

    /**
     * Удалить все счётчики аккаунта
     */
    @Query("DELETE FROM meters WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: String)
}
