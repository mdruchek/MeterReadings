package ru.dr.meterreadings.models.domain

import ru.dr.meterreadings.data.local.entities.AppSettingsEntity

/**
 * Доменная модель глобальных настроек приложения.
 *
 * Это первичная модель — значения по умолчанию определяются здесь,
 * затем они переносятся в Entity при конвертации .toEntity().
 */
data class AppSettingsDomainModel(
    val id: Long = 1L,

    // ============================================
    // УВЕДОМЛЕНИЯ
    // ============================================

    /**
     * Глобальный мастер-флаг уведомлений.
     * Если false — все уведомления отключены.
     */
    val globalNotificationsEnabled: Boolean = false,

    /**
     * Глобальный флаг уведомлений провайдеров.
     * Если false — уведомления для всех провайдеров отключены.
     */
    val providerNotificationsEnabled: Boolean = false,

    // ============================================
    // НАПОМИНАНИЯ
    // ============================================

    /**
     * Глобальный мастер-флаг напоминаний.
     * Если false — все напоминания отключены.
     */
    val globalRemindersEnabled: Boolean = false,

    /**
     * Время напоминания: час (0-23).
     */
    val reminderTimeHour: Int = 9,

    /**
     * Время напоминания: минута (0-59).
     */
    val reminderTimeMinute: Int = 0,

    /**
     * Режим периода напоминаний:
     * - "AUTO" — автоматически за N дней до начала периода передачи
     * - "MANUAL" — индивидуальные настройки для каждого провайдера
     */
    val reminderPeriodMode: String = "AUTO",

    /**
     * За сколько дней до начала периода передачи напоминать (режим AUTO).
     *
     * Например:
     * - transmissionPeriodStartDay = 17
     * - reminderDaysBeforeStart = 3
     * - Напоминания начинаются с 14 числа (17 - 3 = 14)
     */
    val reminderDaysBeforeStart: Int = 0,

    // ============================================
    // СЛУЖЕБНЫЕ ПОЛЯ
    // ============================================

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ============================================
// MAPPERS (конвертация Domain ↔ Entity)
// ============================================

/**
 * Конвертация Domain → Entity (для записи в БД).
 */
fun AppSettingsDomainModel.toEntity(): AppSettingsEntity {
    return AppSettingsEntity(
        id = id,
        globalNotificationsEnabled = globalNotificationsEnabled,
        providerNotificationsEnabled = providerNotificationsEnabled,
        globalRemindersEnabled = globalRemindersEnabled,
        reminderTimeHour = reminderTimeHour,
        reminderTimeMinute = reminderTimeMinute,
        reminderPeriodMode = reminderPeriodMode,
        reminderDaysBeforeStart = reminderDaysBeforeStart,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
