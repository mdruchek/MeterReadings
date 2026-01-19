// app/src/main/java/ru/dr/meterreadings/data/local/entities/AppSettingsEntity.kt
package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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

    // ============================================
    // ТЕХНИЧЕСКИЕ ПОЛЯ
    // ============================================
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
