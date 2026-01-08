package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type

/**
 * Entity (таблица) для провайдеров услуг в базе данных
 *
 * Содержит информацию о компаниях-поставщиках услуг
 */
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey
    val id: String,
    // Название компании (например: "Мосводоканал")
    val name: String,
    // ✅ ИЗМЕНЕНИЕ: Тип услуги как строка (для хранения в БД)
    // Храним enum как String, потому что Room не умеет напрямую хранить enum
    val type: String,
    // URL логотипа компании (может быть null)
    val logoUrl: String? = null,
    // Базовый URL сайта для парсинга
    val baseUrl: String,
    // Тип аутентификации (тоже как строка)
    val authType: String,
    // Технические поля
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
        // ✅ ИЗМЕНЕНИЕ: Конвертируем строку обратно в enum Type
        type = Type.valueOf(type),
        logoUrl = logoUrl,
        baseUrl = baseUrl,
        // Конвертируем строку обратно в enum AuthType
        authType = AuthType.valueOf(authType)
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
        // ✅ ИЗМЕНЕНИЕ: Конвертируем enum Type в строку для БД
        // Type.WaterSupply → "WaterSupply"
        type = type.name,
        logoUrl = logoUrl,
        baseUrl = baseUrl,
        // Конвертируем enum AuthType в строку
        authType = authType.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
