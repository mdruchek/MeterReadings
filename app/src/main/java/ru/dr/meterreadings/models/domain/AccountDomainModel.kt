package ru.dr.meterreadings.models.domain

data class AccountDomainModel(
    val id: String,
    val profileId: String,
    val providerId: String,
    val accountNumber: String,
    val address: String? = null,
    val lastUpdated: Long? = null
)
