// app/src/main/java/ru/dr/meterreadings/data/repository/AppSettingsRepository.kt
package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.AppSettingsDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.models.domain.toEntity
import ru.dr.meterreadings.models.domain.AppSettingsDomainModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с глобальными настройками приложения.
 *
 * Инкапсулирует логику работы с БД и предоставляет удобные методы
 * для ViewModel. Работает с DomainModel, скрывая детали Entity от верхних слоёв.
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {

    // ========================================
    // ЧТЕНИЕ (READ)
    // ========================================

    /**
     * Получить настройки приложения как Flow (DomainModel).
     *
     * Автоматически создаёт настройки по умолчанию при первом запуске.
     * UI подписывается на этот Flow и получает обновления автоматически.
     *
     * @return Flow с настройками (никогда не null)
     */
    fun getSettings(): Flow<AppSettingsDomainModel> {
        return appSettingsDao.getSettings().map { entity ->
            // Если настроек нет (первый запуск), создаём их
            if (entity == null) {
                createDefaultSettings()
            } else {
                // Конвертируем Entity → Domain
                entity.toDomain()
            }
        }
    }

    /**
     * Получить настройки синхронно (для suspend функций).
     *
     * Используется внутри Repository для обновлений.
     *
     * @return Настройки (создаёт по умолчанию, если ещё нет)
     */
    private suspend fun getSettingsSync(): AppSettingsDomainModel {
        val entity = appSettingsDao.getSettingsSync()
        return if (entity == null) {
            createDefaultSettings()
        } else {
            entity.toDomain()
        }
    }

    // ========================================
    // ЗАПИСЬ (WRITE)
    // ========================================

    /**
     * Обновить флаг глобальных уведомлений.
     *
     * Если уведомления отключаются (enabled = false), то все уведомления
     * приложения перестают показываться, независимо от настроек провайдеров.
     *
     * @param enabled true = уведомления включены, false = выключены
     */
    suspend fun updateGlobalNotifications(enabled: Boolean) {
        val entity = appSettingsDao.getSettingsSync()

        if (entity == null) {
            // Настроек нет — создаём с дефолтными значениями из Domain
            val defaultDomain = AppSettingsDomainModel()
            val newDomain = defaultDomain.copy(
                globalNotificationsEnabled = enabled
            )
            appSettingsDao.insert(newDomain.toEntity())
            println("✅ [AppSettingsRepository] Настройки созданы: globalNotifications = $enabled")
        } else {
            appSettingsDao.updateGlobalNotifications(
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Глобальные уведомления обновлены: $enabled")
        }
    }

    /**
     * Обновить флаг уведомлений провайдеров.
     *
     * Включает/выключает уведомления для всех провайдеров.
     *
     * @param enabled true = уведомления провайдеров включены, false = выключены
     */
    suspend fun updateProviderNotifications(enabled: Boolean) {
        val entity = appSettingsDao.getSettingsSync()

        if (entity == null) {
            // Настроек нет — создаём с дефолтными значениями из Domain
            val defaultDomain = AppSettingsDomainModel()
            val newDomain = defaultDomain.copy(
                providerNotificationsEnabled = enabled
            )
            appSettingsDao.insert(newDomain.toEntity())
            println("✅ [AppSettingsRepository] Настройки созданы: providerNotifications = $enabled")
        } else {
            appSettingsDao.updateProviderNotifications(
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Уведомления провайдеров обновлены: $enabled")
        }
    }

    /**
     * Обновить время напоминания.
     *
     * @param hour Час (0-23)
     * @param minute Минута (0-59)
     */
    suspend fun updateReminderTime(hour: Int, minute: Int) {
        val entity = appSettingsDao.getSettingsSync()
        if (entity == null) {
            // Настроек нет — создаём с дефолтными значениями из Domain
            val defaultDomain = AppSettingsDomainModel()
            val newDomain = defaultDomain.copy(
                reminderTimeHour = hour,
                reminderTimeMinute = minute
            )
            appSettingsDao.insert(newDomain.toEntity())
            println("✅ [AppSettingsRepository] Настройки созданы: reminderTime = $hour:$minute")
        } else {
            appSettingsDao.updateReminderTime(
                hour = hour,
                minute = minute,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Время напоминания обновлено: $hour:$minute")
        }
    }

    /**
     * Включить/выключить глобальные напоминания.
     *
     * @param enabled true = напоминания включены, false = выключены
     */
    suspend fun updateGlobalReminders(enabled: Boolean) {
        val entity = appSettingsDao.getSettingsSync()
        if (entity == null) {
            // Настроек нет — создаём с дефолтными значениями из Domain
            val defaultDomain = AppSettingsDomainModel()
            val newDomain = defaultDomain.copy(
                globalRemindersEnabled = enabled
            )
            appSettingsDao.insert(newDomain.toEntity())
            println("✅ [AppSettingsRepository] Настройки созданы: globalReminders = $enabled")
        } else {
            appSettingsDao.updateGlobalReminders(
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Глобальные напоминания обновлены: $enabled")
        }
    }

    /**
     * Изменить режим периода напоминаний.
     *
     * @param mode "AUTO" = автоматически по периоду провайдера, "MANUAL" = вручную
     */
    suspend fun updateReminderPeriodMode(mode: String) {
        val entity = appSettingsDao.getSettingsSync()
        if (entity == null) {
            // Настроек нет — создаём с дефолтными значениями из Domain
            val defaultDomain = AppSettingsDomainModel()
            val newDomain = defaultDomain.copy(
                reminderPeriodMode = mode
            )
            appSettingsDao.insert(newDomain.toEntity())
            println("✅ [AppSettingsRepository] Настройки созданы: reminderPeriodMode = $mode")
        } else {
            appSettingsDao.updateReminderPeriodMode(
                mode = mode,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Режим периода напоминаний обновлён: $mode")
        }
    }

    /**
     * Изменить количество дней до начала периода передачи показаний,
     * за которое показывается напоминание.
     *
     * @param days Количество дней (обычно 1-7)
     */
    suspend fun updateReminderDaysBeforeStart(days: Int) {
        val entity = appSettingsDao.getSettingsSync()
        if (entity == null) {
            // Настроек нет — создаём с дефолтными значениями из Domain
            val defaultDomain = AppSettingsDomainModel()
            val newDomain = defaultDomain.copy(
                reminderDaysBeforeStart = days
            )
            appSettingsDao.insert(newDomain.toEntity())
            println("✅ [AppSettingsRepository] Настройки созданы: reminderDaysBeforeStart = $days")
        } else {
            appSettingsDao.updateReminderDaysBeforeStart(
                days = days,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Дни до начала периода обновлены: $days")
        }
    }


    // ========================================
    // ИНИЦИАЛИЗАЦИЯ
    // ========================================

    /**
     * Создать настройки по умолчанию при первом запуске.
     *
     * Вызывается автоматически из getSettings(), если настроек ещё нет.
     * Создаёт запись в БД и возвращает DomainModel.
     *
     * @return Настройки по умолчанию
     */
    private suspend fun createDefaultSettings(): AppSettingsDomainModel {
        val defaultSettings = AppSettingsDomainModel()
        // Конвертируем Domain → Entity и сохраняем в БД
        appSettingsDao.insert(defaultSettings.toEntity())
        println("✅ [AppSettingsRepository] Настройки по умолчанию созданы")
        return defaultSettings
    }
}
