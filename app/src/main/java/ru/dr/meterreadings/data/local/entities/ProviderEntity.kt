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
    val id: Long,

    // ============================================
    // ОСНОВНАЯ ИНФОРМАЦИЯ
    // ============================================
    val name: String,
    val type: String,
    val logoUrl: String?,
    val baseUrl: String,
    val authType: String,

    // ============================================
    // ПЕРИОД ПЕРЕДАЧИ ПОКАЗАНИЙ
    // ============================================
    val transmissionPeriodStartDay: Int?,
    val transmissionPeriodEndDay: Int?,
    val lastPeriodUpdate: Long?,
    val periodLoadedForMonth: String?,

    // ============================================
    // НАСТРОЙКИ АВТООБНОВЛЕНИЯ
    // ============================================
    val autoUpdateEnabled: Boolean,

    // ============================================
    // НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================
    val notificationsEnabled: Boolean,

    // ============================================
    // НАСТРОЙКИ НАПОМИНАНИЙ
    // ============================================
    val reminderEnabled: Boolean,
    val reminderCustomStartDay: Int?,

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
        notificationsEnabled = notificationsEnabled,

        // Напоминания
        reminderEnabled = reminderEnabled,
        reminderCustomStartDay = reminderCustomStartDay,
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
        notificationsEnabled = notificationsEnabled,

        // Напоминания
        reminderEnabled = reminderEnabled,
        reminderCustomStartDay = reminderCustomStartDay,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
