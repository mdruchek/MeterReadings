// app/src/main/java/ru/dr/meterreadings/data/repository/AppSettingsRepository.kt
package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.AppSettingsDao
import ru.dr.meterreadings.data.local.entities.AppSettingsEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с глобальными настройками приложения.
 *
 * Инкапсулирует логику работы с БД и предоставляет удобные методы
 * для ViewModel. Гарантирует, что настройки всегда существуют в БД.
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {

    // ========================================
    // ЧТЕНИЕ (READ)
    // ========================================

    /**
     * Получить настройки приложения как Flow.
     *
     * Автоматически создаёт настройки по умолчанию при первом запуске.
     * UI подписывается на этот Flow и получает обновления автоматически.
     *
     * @return Flow с настройками (никогда не null)
     */
    fun getSettings(): Flow<AppSettingsEntity> {
        return appSettingsDao.getSettings().map { settings ->
            // Если настроек ещё нет (первый запуск), создаём их
            settings ?: createDefaultSettings()
        }
    }

    /**
     * Получить настройки синхронно (для suspend функций).
     *
     * Используется внутри Repository, когда нужно одноразово
     * прочитать настройки, а не подписываться на изменения.
     *
     * @return Настройки (создаёт по умолчанию, если ещё нет)
     */
    private suspend fun getSettingsSync(): AppSettingsEntity {
        val settings = appSettingsDao.getSettingsSync()
        // Если настроек нет, создаём и возвращаем дефолтные
        return settings ?: createDefaultSettings()
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
        // Проверяем, существуют ли настройки в БД
        val settings = appSettingsDao.getSettingsSync()

        if (settings == null) {
            // Настроек нет — создаём с нужным значением флага
            val newSettings = AppSettingsEntity(
                globalNotificationsEnabled = enabled
            )
            appSettingsDao.insert(newSettings)
            println("✅ [AppSettingsRepository] Настройки созданы: globalNotifications = $enabled")
        } else {
            // Настройки есть — обновляем только флаг
            appSettingsDao.updateGlobalNotifications(
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            println("✅ [AppSettingsRepository] Глобальные уведомления обновлены: $enabled")
        }
    }

    // ========================================
    // ИНИЦИАЛИЗАЦИЯ
    // ========================================

    /**
     * Создать настройки по умолчанию при первом запуске.
     *
     * Вызывается автоматически из getSettings(), если настроек ещё нет.
     * Создаёт запись в БД и возвращает её.
     *
     * @return Настройки по умолчанию
     */
    private suspend fun createDefaultSettings(): AppSettingsEntity {
        val defaultSettings = AppSettingsEntity(
            globalNotificationsEnabled = false // Значение по умолчанию из Entity
        )
        appSettingsDao.insert(defaultSettings)
        println("✅ [AppSettingsRepository] Настройки по умолчанию созданы")
        return defaultSettings
    }
}
