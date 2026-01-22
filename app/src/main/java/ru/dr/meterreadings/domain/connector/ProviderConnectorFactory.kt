package ru.dr.meterreadings.domain.connector

import ru.dr.meterreadings.data.repository.providers.kvc.KvcConnector
import ru.dr.meterreadings.domain.constants.ProviderIds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderConnectorFactory @Inject constructor(
    private val kvcConnector: KvcConnector
) {

    fun getConnector(providerId: Long): ProviderConnector {
        return when (providerId) {
            ProviderIds.KVC -> kvcConnector
            else -> throw IllegalArgumentException(
                "Провайдер с ID=$providerId не поддерживается"
            )
        }
    }
}

