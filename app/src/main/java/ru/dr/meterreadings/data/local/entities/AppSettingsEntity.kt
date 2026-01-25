package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.dr.meterreadings.models.domain.AppSettingsDomainModel
import kotlin.Boolean
import kotlin.Int

/**
 * Entity для глобальных настроек приложения.
 *
 * Значения по умолчанию НЕ задаются здесь — они определены в AppSettingsDomainModel
 * и попадают в БД через конвертацию .toEntity().
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Long = 1L,

    // ============================================
    // УВЕДОМЛЕНИЯ
    // ============================================

    /**
     * Глобальный мастер-флаг уведомлений.
     */
    val globalNotificationsEnabled: Boolean,

    /**
     * Глобальный флаг уведомлений провайдеров.
     */
    val providerNotificationsEnabled: Boolean,

    // ============================================
    // НАПОМИНАНИЯ
    // ============================================

    /**
     * Глобальный мастер-флаг напоминаний.
     */
    val globalRemindersEnabled: Boolean,

    /**
     * Время напоминания: час (0-23).
     */
    val reminderTimeHour: Int,

    /**
     * Время напоминания: минута (0-59).
     */
    val reminderTimeMinute: Int,

    /**
     * Режим периода напоминаний: "AUTO" или "MANUAL".
     */
    val reminderPeriodMode: String,

    /**
     * За сколько дней до начала периода передачи напоминать (режим AUTO).
     */
    val reminderDaysBeforeStart: Int,

    // ============================================
    // ЛОГИ
    // ============================================
    val loggingEnabled: Boolean,
    val logRetentionDays: Int,

    // ============================================
    // АВТООБНОВЛЕНИЕ (ГЛОБАЛЬНОЕ!)
    // ============================================
    val autoUpdateIntervalHours: Int,
    val autoUpdateStartDay: Int,

    // ============================================
    // СЛУЖЕБНЫЕ ПОЛЯ
    // ============================================

    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Конвертация Entity → Domain (для бизнес-логики).
 */
fun AppSettingsEntity.toDomain(): AppSettingsDomainModel {
    return AppSettingsDomainModel(
        id = id,
        globalNotificationsEnabled = globalNotificationsEnabled,
        providerNotificationsEnabled = providerNotificationsEnabled,
        globalRemindersEnabled = globalRemindersEnabled,
        reminderTimeHour = reminderTimeHour,
        reminderTimeMinute = reminderTimeMinute,
        reminderPeriodMode = reminderPeriodMode,
        reminderDaysBeforeStart = reminderDaysBeforeStart,
        loggingEnabled = loggingEnabled,
        logRetentionDays = logRetentionDays,
        autoUpdateIntervalHours = autoUpdateIntervalHours,
        autoUpdateStartDay = autoUpdateStartDay,

        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
