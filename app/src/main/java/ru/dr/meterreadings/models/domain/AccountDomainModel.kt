package ru.dr.meterreadings.models.domain

import ru.dr.meterreadings.domain.connector.GetAccounts
import java.util.UUID

data class AccountDomainModel(
    val id: String,
    val profileId: String,
    val providerId: Long,
    val number: String,
    val uuid: String?,

    // ========================================
    // Дополнительные поля для провайдеров
    // ========================================

    /**
     * ID региона провайдера (для КВЦ и других с регионами)
     *
     * Null для провайдеров без регионов (ТНС, Газпром)
     */
    val regionId: String? = null,

    /**
     * Логин для входа в личный кабинет
     *
     * Используется для:
     * - ТНС: получение токена при реавторизации
     * - KVC: может использоваться для автозаполнения
     * - Другие провайдеры с авторизацией
     */
    val login: String? = null,
    val address: String? = null,
)

fun GetAccounts.AccountInfo.toAccountDomainModel(
    profileId: String,
    providerId: Long,
): AccountDomainModel {
    return AccountDomainModel(
        id = UUID.randomUUID().toString(),
        profileId = profileId,
        providerId = providerId,
        number = this.number,
        uuid = this.uuid,
        address = this.address,
        regionId = this.regionId,
        login = this.login
    )
}
