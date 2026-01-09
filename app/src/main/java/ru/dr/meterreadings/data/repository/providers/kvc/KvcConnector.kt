package ru.dr.meterreadings.data.repository.providers.kvc

import ru.dr.meterreadings.domain.connector.*
import ru.dr.meterreadings.data.remote.dto.KvcAbonentInfoDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Коннектор для провайдера КВЦ (Нижний Новгород)
 */
@Singleton
class KvcConnector @Inject constructor(
    private val kvcRepository: KvcRepository
) : ProviderConnector,
    HasRegions,
    SearchAccount,
    SubmitReadings {

    override val providerId: Long = 2L
    override val providerName: String = "КВЦ"

    // Кэш для сохранения данных между запросами
    private val cache = mutableMapOf<String, KvcAbonentInfoDto>()

    // ========================================
    // HasRegions
    // ========================================

    override suspend fun getRegions(): Result<List<HasRegions.RegionInfo>> {
        return kvcRepository.getRegions().map { regions ->
            regions.map { region ->
                HasRegions.RegionInfo(
                    id = region.id.toString(),
                    name = region.name
                )
            }
        }
    }

    // ========================================
    // SearchAccount
    // ========================================

    override suspend fun searchAccount(
        accountNumber: String,
        regionId: String?
    ): Result<String> {
        if (regionId == null) {
            return Result.failure(Exception("Необходимо выбрать регион"))
        }

        try {
            // 1. Получаем БД региона
            val locationsResult = kvcRepository.getLocationsForRegion(regionId.toInt())
            if (locationsResult.isFailure) {
                return Result.failure(locationsResult.exceptionOrNull()!!)
            }

            val locations = locationsResult.getOrThrow()

            // 2. Ищем абонента
            val abonentResult = kvcRepository.getAbonentInfo(locations, accountNumber)
            if (abonentResult.isFailure) {
                return Result.failure(abonentResult.exceptionOrNull()!!)
            }

            val abonent = abonentResult.getOrThrow()

            // 3. Сохраняем в кэш для последующих операций
            cache[accountNumber] = abonent

            // 4. Возвращаем только адрес
            return Result.success(abonent.getFullAddress())

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // ========================================
    // SubmitReadings
    // ========================================

    override suspend fun submitReading(
        counterId: String,
        accountNumber: String,
        value: String,
        valueNight: String?,
        regionId: String?
    ): Result<Unit> {
        try {
            // Получаем абонента из кэша
            val abonent = cache[accountNumber]
                ?: return Result.failure(Exception("Сначала нужно выполнить поиск абонента"))

            // Получаем счётчики
            val countersResult = kvcRepository.getCounters(
                location = abonent.location,
                accountNumber = accountNumber
            )

            if (countersResult.isFailure) {
                return Result.failure(countersResult.exceptionOrNull()!!)
            }

            // Находим нужный счётчик
            val counter = countersResult.getOrThrow()
                .firstOrNull { it.idCnt.toString() == counterId }
                ?: return Result.failure(Exception("Счётчик не найден"))

            // Отправляем показания
            return kvcRepository.submitReading(
                counter = counter,
                location = abonent.location,
                value = value,
                valueNight = valueNight
            )

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
