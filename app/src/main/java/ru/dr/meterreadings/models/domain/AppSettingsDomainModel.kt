// app/src/main/java/ru/dr/meterreadings/models/domain/AppSettingsDomainModel.kt
package ru.dr.meterreadings.models.domain

/**
 * Domain-модель глобальных настроек приложения.
 *
 * Используется в бизнес-логике (Repository, ViewModel).
 * Не содержит технических полей БД (timestamps), только бизнес-данные.
 *
 * Отличие от Entity:
 * - Entity = структура БД (с timestamps, constraints)
 * - Domain = чистая бизнес-логика
 */
data class AppSettingsDomainModel(
    // ============================================
    // ГЛОБАЛЬНЫЕ НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================

    /**
     * Мастер-переключатель всех уведомлений приложения.
     * Если false — никакие уведомления не показываются.
     */
    val globalNotificationsEnabled: Boolean,

    /**
     * Глобальный флаг для уведомлений провайдеров.
     * Если false — уведомления провайдеров (напоминания, успех, ошибки)
     * отключены независимо от индивидуальных настроек провайдеров.
     */
    val providerNotificationsEnabled: Boolean
)
