package ru.dr.meterreadings.models.ui

import ru.dr.meterreadings.models.domain.ProfileDomainModel

data class ProfileUiModel(
    val profile: ProfileDomainModel,
    val accountCount: Int,
    val addressCount: Int,
    val readingsCount: Int,
    val lastUpdateDate: String? = null  // форматированная дата
)
