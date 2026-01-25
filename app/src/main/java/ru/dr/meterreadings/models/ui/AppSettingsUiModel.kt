// app/src/main/java/ru/dr/meterreadings/models/ui/AppSettingsUiModel.kt
package ru.dr.meterreadings.models.ui

import ru.dr.meterreadings.models.domain.AppSettingsDomainModel

/**
 * UI-модель глобальных настроек приложения.
 *
 * Используется в Composable-функциях для отображения.
 * Содержит дополнительные поля для UI-состояния (например, тексты для отображения).
 *
 * Отличие от Domain:
 * - Domain = чистые данные
 * - UI = данные + вспомогательная информация для отображения
 */
data class AppSettingsUiModel(
    val globalNotificationsEnabled: Boolean,
    val providerNotificationsEnabled: Boolean,
    val loggingEnabled: Boolean,
    val logRetentionDays: Int
) {
    /**
     * Текст состояния глобальных уведомлений для UI.
     * Вычисляемое свойство — не хранится, генерируется на лету.
     */
    val globalNotificationsStatus: String
        get() = if (globalNotificationsEnabled) {
            "Уведомления включены для всего приложения"
        } else {
            "Уведомления отключены полностью"
        }

    /**
     * Текст состояния уведомлений провайдеров для UI.
     */
    val providerNotificationsStatus: String
        get() = if (providerNotificationsEnabled) {
            "Уведомления для всех провайдеров включены"
        } else {
            "Уведомления провайдеров отключены"
        }
}

// ========================================
// МАППИНГ: Domain ↔ UI
// ========================================

/**
 * Конвертирует DomainModel в UiModel для отображения.
 */
fun AppSettingsDomainModel.toUi(): AppSettingsUiModel {
    return AppSettingsUiModel(
        globalNotificationsEnabled = globalNotificationsEnabled,
        providerNotificationsEnabled = providerNotificationsEnabled,
        loggingEnabled = loggingEnabled,
        logRetentionDays = logRetentionDays,
    )
}

/**
 * Конвертирует UiModel обратно в DomainModel (редко используется).
 */
fun AppSettingsUiModel.toDomain(): AppSettingsDomainModel {
    return AppSettingsDomainModel(
        globalNotificationsEnabled = globalNotificationsEnabled,
        providerNotificationsEnabled = providerNotificationsEnabled,
        loggingEnabled = loggingEnabled,
        logRetentionDays = logRetentionDays,
    )
}
