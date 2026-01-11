package ru.dr.meterreadings.data.repository.providers.kvc

import ru.dr.meterreadings.domain.connector.HasRegions
import ru.dr.meterreadings.domain.connector.ProviderConnector
import ru.dr.meterreadings.domain.connector.SearchAccount
import ru.dr.meterreadings.domain.connector.SubmitReadings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KvcConnector @Inject constructor(
    private val kvcRepository: KvcRepository
) : ProviderConnector,
    HasRegions,
    SearchAccount,
    SubmitReadings {

    override val providerId: Long = 1L
    override val providerName: String = "КВЦ"

    /**
     * Получить список регионов КВЦ
     */
    override suspend fun getRegions(): Result<List<HasRegions.RegionInfo>> {
        println("🌐 [KvcConnector] Запрос регионов...")

        // ✅ Repository возвращает Result - обрабатываем его
        val result = kvcRepository.getRegions()

        return result.fold(
            onSuccess = { regions ->
                println("✅ [KvcConnector] Получено регионов: ${regions.size}")

                val regionInfoList = regions.map { region ->
                    HasRegions.RegionInfo(
                        id = region.id.toString(),
                        name = region.name
                    )
                }

                Result.success(regionInfoList)
            },
            onFailure = { error ->
                println("❌ [KvcConnector] Ошибка: ${error.message}")
                Result.failure(error)
            }
        )
    }

    /**
     * Поиск адреса абонента
     */
    override suspend fun searchAccount(
        accountNumber: String,
        regionId: String?
    ): Result<String> {
        println("🔍 [KvcConnector] Поиск ЛС $accountNumber в регионе $regionId")

        return try {
            val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                "Для КВЦ необходимо указать регион"
            }

            // Получаем конфигурации БД для региона
            val locationsResult = kvcRepository.getLocationsForRegion(regionIdInt)

            if (locationsResult.isFailure) {
                return Result.failure(
                    locationsResult.exceptionOrNull()
                        ?: Exception("Не удалось загрузить конфигурации БД")
                )
            }

            val locations = locationsResult.getOrThrow()

            // Ищем абонента
            val abonentResult = kvcRepository.getAbonentInfo(
                locations = locations,
                accountNumber = accountNumber,
                target = 0
            )

            abonentResult.fold(
                onSuccess = { abonentInfo ->
                    val address = abonentInfo.getFullAddress()
                    println("✅ [KvcConnector] Адрес найден: $address")
                    Result.success(address)
                },
                onFailure = { error ->
                    println("❌ [KvcConnector] Ошибка поиска: ${error.message}")
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            println("❌ [KvcConnector] Ошибка: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Передача показаний
     */
    override suspend fun submitReading(
        counterId: String,
        accountNumber: String,
        value: String,
        valueNight: String?,
        regionId: String?
    ): Result<Unit> {
        // TODO: Реализовать позже используя kvcRepository.submitReading()
        return Result.failure(NotImplementedError("Передача показаний ещё не реализована"))
    }
}
