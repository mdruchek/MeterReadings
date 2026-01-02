package ru.dr.meterreadings.models.ui

import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.MeterDomainModel

data class AccountUiModel (
    val account: AccountDomainModel,
    val provider: ProviderDomainModel,
    val meters: List<MeterDomainModel> = emptyList()
)