package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.dr.meterreadings.models.domain.AccountDomainModel

/**
 * Entity (таблица) для личного кабинета в компании
 *
 * Room создаст таблицу "accounts" с внешним ключом на profiles
 *
 * ForeignKey - связь с таблицей profiles:
 * - onDelete = CASCADE - при удалении профиля удалятся все его accounts
 *
 * Index - индекс для быстрого поиска
 */
@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["providerId"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    // Внешний ключ на Profile
    val profileId: String,
    // ID компании (Provider)
    val providerId: Long,
    // Номер лицевого счета
    val accountNumber: String,
    // UUID аккаунта
    val accountUUID: String?,
    // Логин для входа в личный кабинет (может быть null)
    val login: String?,
    // Пароль (зашифрованный, может быть null)
    val password: String? = null,
    // Технические поля
    val createdAt: Long,
    val updatedAt: Long,
    // ========================================
    // Дополнительные поля для провайдеров
    // ========================================

    /**
     * ID региона провайдера (для КВЦ)
     *
     * Nullable для провайдеров без регионов
     */
    val regionId: String?,
)

// ========================================
// МАППИНГ: Entity ↔ Domain
// ========================================

/**
 * Конвертирует Entity (из БД) в DomainModel
 */
fun AccountEntity.toDomain(): AccountDomainModel {
    return AccountDomainModel(
        id = id,
        profileId = profileId,
        providerId = providerId,
        number = accountNumber,
        uuid = accountUUID,
        regionId = regionId,
        login = login,
        address = null
    )
}

/**
 * Конвертирует DomainModel в Entity (для сохранения в БД)
 */
fun AccountDomainModel.toEntity(
    login: String? = null,
    password: String? = null,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): AccountEntity {
    return AccountEntity(
        id = id,
        profileId = profileId,
        providerId = providerId,
        accountNumber = number,
        accountUUID = uuid,
        regionId = regionId,
        login = login,
        password = password,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
