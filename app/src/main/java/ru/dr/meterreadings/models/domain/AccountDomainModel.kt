package ru.dr.meterreadings.models.domain

data class AccountDomainModel(
    val id: String,
    val profileId: String,
    val providerId: Long,
    val accountNumber: String,

    // ========================================
    // Дополнительные поля для провайдеров
    // ========================================

    /**
     * ID региона провайдера (для КВЦ и других с регионами)
     *
     * Null для провайдеров без регионов (ТНС, Газпром)
     */
    val regionId: Int? = null
)
