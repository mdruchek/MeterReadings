// app/src/main/java/ru/dr/meterreadings/data/local/entities/AppSettingsEntity.kt
package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.dr.meterreadings.models.domain.AppSettingsDomainModel

/**
 * Entity для глобальных настроек приложения.
 *
 * В БД будет всегда одна строка с id = 1 (Singleton pattern).
 * Все глобальные настройки хранятся здесь, а настройки конкретных
 * провайдеров остаются в ProviderEntity.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Всегда 1, т.к. настройки приложения единственные

    // ============================================
    // ГЛОБАЛЬНЫЕ НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================

    /**
     * Мастер-переключатель всех уведомлений приложения.
     * Если false — никакие уведомления не показываются,
     * независимо от настроек провайдеров.
     */
    val globalNotificationsEnabled: Boolean = false,

    // Глобальный флаг для уведомлений провайдеров
    val providerNotificationsEnabled: Boolean = false,

    // ============================================
    // ТЕХНИЧЕСКИЕ ПОЛЯ
    // ============================================
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ========================================
// МАППИНГ: Entity ↔ Domain
// ========================================

/**
 * Конвертирует Entity (из БД) в DomainModel (для бизнес-логики).
 */
fun AppSettingsEntity.toDomain(): AppSettingsDomainModel {
    return AppSettingsDomainModel(
        globalNotificationsEnabled = globalNotificationsEnabled,
        providerNotificationsEnabled = providerNotificationsEnabled
    )
}

/**
 * Конвертирует DomainModel в Entity (для сохранения в БД).
 *
 * @param createdAt Метка создания (по умолчанию текущее время)
 * @param updatedAt Метка обновления (по умолчанию текущее время)
 */
fun AppSettingsDomainModel.toEntity(
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): AppSettingsEntity {
    return AppSettingsEntity(
        id = 1, // Всегда 1 для Singleton
        globalNotificationsEnabled = globalNotificationsEnabled,
        providerNotificationsEnabled = providerNotificationsEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

