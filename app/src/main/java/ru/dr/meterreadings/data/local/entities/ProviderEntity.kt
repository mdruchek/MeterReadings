// app/src/main/java/ru/dr/meterreadings/data/local/entities/ProviderEntity.kt

package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type

/**
 * Entity (таблица) для провайдеров услуг в базе данных
 */
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey
    val id: String,

    // ============================================
    // ОСНОВНАЯ ИНФОРМАЦИЯ
    // ============================================
    val name: String,
    val type: String,              // Type.name (для Room)
    val logoUrl: String? = null,
    val baseUrl: String,
    val authType: String,          // AuthType.name (для Room)

    // ============================================
    // ПЕРИОД ПЕРЕДАЧИ ПОКАЗАНИЙ
    // ============================================
    val transmissionPeriodStartDay: Int? = null,
    val transmissionPeriodEndDay: Int? = null,
    val lastPeriodUpdate: Long? = null,
    val periodLoadedForMonth: String? = null,

    // ============================================
    // НАСТРОЙКИ АВТООБНОВЛЕНИЯ
    // ============================================
    val autoUpdateEnabled: Boolean = false,
    val updateStartDay: Int = 1,
    val updateIntervalHours: Int = 1,
    val lastAutoUpdate: Long? = null,              // ✨ НОВОЕ

    // ============================================
    // НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================
    val notificationsEnabled: Boolean = false,

    // ============================================
    // НАСТРОЙКИ НАПОМИНАНИЙ
    // ============================================
    val reminderEnabled: Boolean = false,
    val reminderTimeHour: Int = 9,
    val reminderTimeMinute: Int = 0,
    val reminderPeriodMode: String = "AUTO",       // ✨ НОВОЕ
    val reminderCustomStartDay: Int? = null,       // ✨ НОВОЕ
    val reminderCustomEndDay: Int? = null,         // ✨ НОВОЕ

    // ============================================
    // ТЕХНИЧЕСКИЕ ПОЛЯ
    // ============================================
    val createdAt: Long,
    val updatedAt: Long
)

// ========================================
// МАППИНГ: Entity ↔ Domain
// ========================================

/**
 * Конвертирует Entity (из БД) в DomainModel (для бизнес-логики)
 */
fun ProviderEntity.toDomain(): ProviderDomainModel {
    return ProviderDomainModel(
        id = id,
        name = name,
        type = Type.valueOf(type),
        logoUrl = logoUrl,
        baseUrl = baseUrl,
        authType = AuthType.valueOf(authType),

        // Период передачи
        transmissionPeriodStartDay = transmissionPeriodStartDay,
        transmissionPeriodEndDay = transmissionPeriodEndDay,
        lastPeriodUpdate = lastPeriodUpdate,
        periodLoadedForMonth = periodLoadedForMonth,

        // Автообновление
        autoUpdateEnabled = autoUpdateEnabled,
        updateStartDay = updateStartDay,
        updateIntervalHours = updateIntervalHours,
        lastAutoUpdate = lastAutoUpdate,                   // ✨ НОВОЕ

        // Уведомления
        notificationsEnabled = notificationsEnabled,

        // Напоминания
        reminderEnabled = reminderEnabled,
        reminderTimeHour = reminderTimeHour,
        reminderTimeMinute = reminderTimeMinute,
        reminderPeriodMode = reminderPeriodMode,           // ✨ НОВОЕ
        reminderCustomStartDay = reminderCustomStartDay,   // ✨ НОВОЕ
        reminderCustomEndDay = reminderCustomEndDay        // ✨ НОВОЕ
    )
}

/**
 * Конвертирует DomainModel в Entity (для сохранения в БД)
 */
fun ProviderDomainModel.toEntity(
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): ProviderEntity {
    return ProviderEntity(
        id = id,
        name = name,
        type = type.name,
        logoUrl = logoUrl,
        baseUrl = baseUrl,
        authType = authType.name,

        // Период передачи
        transmissionPeriodStartDay = transmissionPeriodStartDay,
        transmissionPeriodEndDay = transmissionPeriodEndDay,
        lastPeriodUpdate = lastPeriodUpdate,
        periodLoadedForMonth = periodLoadedForMonth,

        // Автообновление
        autoUpdateEnabled = autoUpdateEnabled,
        updateStartDay = updateStartDay,
        updateIntervalHours = updateIntervalHours,
        lastAutoUpdate = lastAutoUpdate,                   // ✨ НОВОЕ

        // Уведомления
        notificationsEnabled = notificationsEnabled,

        // Напоминания
        reminderEnabled = reminderEnabled,
        reminderTimeHour = reminderTimeHour,
        reminderTimeMinute = reminderTimeMinute,
        reminderPeriodMode = reminderPeriodMode,           // ✨ НОВОЕ
        reminderCustomStartDay = reminderCustomStartDay,   // ✨ НОВОЕ
        reminderCustomEndDay = reminderCustomEndDay,       // ✨ НОВОЕ

        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
