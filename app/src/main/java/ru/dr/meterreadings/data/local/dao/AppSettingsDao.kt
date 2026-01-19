// app/src/main/java/ru/dr/meterreadings/data/local/dao/AppSettingsDao.kt
package ru.dr.meterreadings.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.dr.meterreadings.data.local.entities.AppSettingsEntity

/**
 * DAO для работы с глобальными настройками приложения.
 *
 * Использует Flow для реактивного обновления UI при изменении настроек.
 * В таблице всегда одна строка с id = 1.
 */
@Dao
interface AppSettingsDao {

    /**
     * Получить настройки приложения как Flow.
     *
     * Flow автоматически уведомляет UI об изменениях в БД.
     * Если настроек ещё нет (первый запуск), вернёт null.
     *
     * @return Flow с настройками или null
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    /**
     * Получить настройки приложения синхронно (для suspend функций).
     *
     * Используется в Repository, когда нужно одноразово получить настройки,
     * а не подписываться на изменения.
     *
     * @return Настройки или null, если ещё не созданы
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsSync(): AppSettingsEntity?

    /**
     * Вставить настройки при первом запуске.
     *
     * OnConflictStrategy.REPLACE — если настройки уже есть (id = 1),
     * заменяет их новыми значениями (работает как update).
     *
     * @param settings Новые настройки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AppSettingsEntity)

    /**
     * Обновить существующие настройки.
     *
     * Room автоматически найдёт строку по id = 1 и обновит все поля.
     *
     * @param settings Обновлённые настройки
     */
    @Update
    suspend fun update(settings: AppSettingsEntity)

    /**
     * Обновить только флаг глобальных уведомлений.
     *
     * Это более эффективно, чем обновлять весь объект,
     * если меняется только один флаг.
     *
     * @param enabled Новое значение флага
     */
    @Query("UPDATE app_settings SET globalNotificationsEnabled = :enabled, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateGlobalNotifications(enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
}
