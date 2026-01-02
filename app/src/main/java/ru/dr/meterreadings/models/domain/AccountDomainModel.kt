package ru.dr.meterreadings.models.domain

data class AccountDomainModel(
    val id: String,
    val profileId: String,
    val providerId: String,
    val accountNumber: String
)
