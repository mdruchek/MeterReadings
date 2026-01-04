package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.dr.meterreadings.models.domain.ProfileDomainModel

/**
 * Entity (таблица) для профиля в базе данных
 *
 * Содержит все поля из ProfileDomainModel + технические поля БД
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    val icon: String? = null,

    val isDefault: Boolean = false,

    // Технические поля (только в БД)
    val createdAt: Long,

    val updatedAt: Long
)

// ========================================
// МАППИНГ: Entity ↔ Domain
// ========================================

/**
 * Конвертирует Entity (из БД) в DomainModel (для бизнес-логики)
 */
fun ProfileEntity.toDomain(): ProfileDomainModel {
    return ProfileDomainModel(
        id = id,
        name = name,
        icon = icon,
        isDefault = isDefault
    )
}

/**
 * Конвертирует DomainModel в Entity (для сохранения в БД)
 */
fun ProfileDomainModel.toEntity(
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        icon = icon,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

