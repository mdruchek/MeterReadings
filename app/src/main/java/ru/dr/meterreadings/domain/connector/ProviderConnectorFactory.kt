package ru.dr.meterreadings.domain.connector

import ru.dr.meterreadings.data.repository.providers.kvc.KvcConnector
import ru.dr.meterreadings.data.repository.providers.tns.TnsConnector
import ru.dr.meterreadings.domain.constants.ProviderIds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фабрика для получения коннектора провайдера по его ID
 *
 * Используется для маршрутизации запросов к нужному провайдеру.
 * Каждый коннектор инжектится через Hilt и создаётся один раз (Singleton).
 *
 * Пример использования:
 * ```kotlin
 * val connector = factory.getConnector(ProviderIds.KVC)
 * if (connector is HasRegions) {
 *     val regions = connector.getRegions()
 * }
 * ```
 */
@Singleton
class ProviderConnectorFactory @Inject constructor(
    /**
     * Коннектор для КВЦ (Нижегородская область)
     *
     * Поддерживает:
     * - HasRegions (выбор региона)
     * - SearchAccount (поиск лицевого счёта)
     * - LoadMeters (загрузка счётчиков)
     * - SubmitReadings (отправка показаний)
     * - GetTransmissionPeriod (период передачи)
     * - ValidateReading (валидация показаний)
     * - GetCounterHistory (история показаний)
     */
    private val kvcConnector: KvcConnector,

    /**
     * Коннектор для ТНС Энерго
     *
     * Поддерживает (на текущий момент):
     * - HasRegions (выбор региона)
     *
     * TODO: По мере изучения API будут добавлены:
     * - SearchAccount
     * - LoadMeters
     * - SubmitReadings
     * - GetTransmissionPeriod
     */
    private val tnsConnector: TnsConnector
) {
    /**
     * Получить коннектор провайдера по ID
     *
     * @param providerId ID провайдера из ProviderIds
     * @return Коннектор провайдера
     * @throws IllegalArgumentException если провайдер не поддерживается
     */
    fun getConnector(providerId: Long): ProviderConnector {
        return when (providerId) {
            ProviderIds.KVC -> {
                println("🔌 [ConnectorFactory] Получен коннектор КВЦ")
                kvcConnector
            }

            ProviderIds.TNS -> {
                println("🔌 [ConnectorFactory] Получен коннектор ТНС")
                tnsConnector
            }

            else -> {
                println("❌ [ConnectorFactory] Провайдер с ID=$providerId не поддерживается")
                throw IllegalArgumentException(
                    "Провайдер с ID=$providerId не поддерживается. " +
                            "Доступные: ${ProviderIds.KVC} (КВЦ), ${ProviderIds.TNS} (ТНС)"
                )
            }
        }
    }
}
