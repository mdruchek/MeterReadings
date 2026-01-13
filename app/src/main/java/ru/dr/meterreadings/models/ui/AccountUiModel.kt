package ru.dr.meterreadings.models.ui

import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProviderDomainModel

data class AccountUiModel (
    val account: AccountDomainModel,
    val address: String? = null,
    val lastUpdated: Long? = null
)