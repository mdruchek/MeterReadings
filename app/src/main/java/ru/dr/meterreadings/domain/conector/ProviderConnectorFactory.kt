package ru.dr.meterreadings.domain.connector

import ru.dr.meterreadings.data.repository.providers.kvc.KvcConnector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фабрика коннекторов провайдеров
 */
@Singleton
class ProviderConnectorFactory @Inject constructor(
    private val kvcConnector: KvcConnector
) {

    fun getConnector(providerId: Long): ProviderConnector {
        return when (providerId) {
            2L -> kvcConnector
            else -> throw IllegalArgumentException("Провайдер с ID=$providerId не поддерживается")
        }
    }
}
