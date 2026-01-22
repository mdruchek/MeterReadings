package ru.dr.meterreadings.data.repository.providers.kvc

import ru.dr.meterreadings.data.mappers.KvcPeriodMapper
import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
import ru.dr.meterreadings.data.remote.dto.KvcLocationDto
import ru.dr.meterreadings.domain.connector.GetTransmissionPeriod
import ru.dr.meterreadings.domain.connector.HasRegions
import ru.dr.meterreadings.domain.connector.LoadMeters
import ru.dr.meterreadings.domain.connector.ProviderConnector
import ru.dr.meterreadings.domain.connector.SearchAccount
import ru.dr.meterreadings.domain.connector.SubmitReadings
import ru.dr.meterreadings.domain.constants.ProviderIds
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KvcConnector @Inject constructor(
    private val kvcRepository: KvcRepository
) : ProviderConnector,
    HasRegions,
    SearchAccount,
    SubmitReadings,
    GetTransmissionPeriod,
    LoadMeters {

    override val providerId: Long = ProviderIds.KVC
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
     * Загрузка счётчиков для аккаунта КВЦ
     */
    override suspend fun loadMeters(
        accountNumber: String,
        regionId: String?
    ): Result<LoadMeters.LoadMetersResult> {
        return try {
            val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                "Для КВЦ необходимо указать регион"
            }

            println("🔍 [KvcConnector] Загрузка счётчиков для ЛС $accountNumber")

            // ШАГ 1: Конфигурации БД
            val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                .getOrElse { return Result.failure(it) }

            // ШАГ 2: Ищем абонента
            val abonentInfo = kvcRepository.getAbonentInfo(
                locations = locations,
                accountNumber = accountNumber,
                target = 0
            ).getOrElse { return Result.failure(it) }

            val address = abonentInfo.getFullAddress()

            // ШАГ 3: Получаем счётчики
            val kvcCounters = kvcRepository.getCounters(
                location = abonentInfo.location,
                accountNumber = accountNumber
            ).getOrElse { return Result.failure(it) }

            // ШАГ 4: Маппинг в универсальный формат
            val meters = kvcCounters
                .filter { it.canEdit() }
                .map { counter ->
                    LoadMeters.MeterInfo(
                        id = counter.idCnt.toString(),
                        type = if (counter.idTtype == "2T") {
                            "${counter.servName.trim()} (${counter.idTtype})"
                        } else {
                            counter.servName.trim()
                        },
                        serialNumber = counter.number.trim(),
                        lastValue = parseValueAsInt(counter.cValLst),
                        lastUpdateTimestamp = parseTimestamp(counter.datLst),
                        lastSubmissionDate = parseSubmissionDate(counter.datLst),
                        apiCounterId = counter.idCnt
                    )
                }

            println("✅ [KvcConnector] Загружено ${meters.size} счётчиков")

            Result.success(
                LoadMeters.LoadMetersResult(
                    meters = meters,
                    address = address,
                    cacheData = mapOf(
                        "location" to abonentInfo.location,
                        "counters" to kvcCounters
                    )
                )
            )
        } catch (e: Exception) {
            println("❌ [KvcConnector] Ошибка: ${e.message}")
            Result.failure(e)
        }
    }

    // Вспомогательные функции
    private fun parseValueAsInt(value: String): Int? {
        if (value.isBlank() || value == "0") return null
        return try {
            val cleaned = value.trim().replace(",", ".")
            cleaned.toDoubleOrNull()?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTimestamp(dateString: String): Long? {
        if (dateString.isBlank()) return null
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        )
        for (format in formats) {
            try {
                return format.parse(dateString.trim())?.time
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun parseSubmissionDate(dateString: String): String? {
        if (dateString.isBlank()) return null
        return try {
            val datePart = dateString.substringBefore("T")
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                "$day.$month.$year"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // app/src/main/java/ru/dr/meterreadings/data/repository/providers/kvc/KvcConnector.kt

    /**
     * Передача показаний
     */
    override suspend fun submitReading(
        counterId: String,
        accountNumber: String,
        value: String,
        valueNight: String?,
        regionId: String?,
        cacheData: Any?
    ): Result<Unit> {
        return try {
            println("📤 [KvcConnector] Отправка показания: счётчик $counterId = $value")

            // ✅ Пытаемся использовать кеш из ViewModel
            val cache = cacheData as? Map<String, Any>

            val location: KvcLocationDto
            val kvcCounters: List<KvcCounterDto>

            if (cache != null && cache.containsKey("location") && cache.containsKey("counters")) {
                // ✅ Используем кеш из памяти
                println("✅ [KvcConnector] Используем кеш из ViewModel")
                location = cache["location"] as KvcLocationDto
                kvcCounters = cache["counters"] as List<KvcCounterDto>
            } else {
                // ❌ Кеш пуст — загружаем заново через API
                println("⚠️ [KvcConnector] Кеш пуст, загружаем данные через API")

                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                    "Для КВЦ необходимо указать регион"
                }

                // ШАГ 1: Конфигурации БД
                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }

                // ШАГ 2: Информация об абоненте
                val abonentInfo = kvcRepository.getAbonentInfo(
                    locations = locations,
                    accountNumber = accountNumber,
                    target = 0
                ).getOrElse { return Result.failure(it) }

                location = abonentInfo.location

                // ШАГ 3: Счётчики
                kvcCounters = kvcRepository.getCounters(
                    location = location,
                    accountNumber = accountNumber
                ).getOrElse { return Result.failure(it) }
            }

            // ШАГ 4: Находим счётчик
            val counter = kvcCounters.firstOrNull { it.idCnt.toString() == counterId }
                ?: return Result.failure(Exception("Счётчик с ID $counterId не найден"))

            println("📋 [KvcConnector] Найден счётчик: ${counter.servName} №${counter.number}")

            // ШАГ 5: Отправляем
            val result = kvcRepository.submitReading(
                counter = counter,
                location = location,
                value = value,
                valueNight = valueNight
            )

            if (result.isSuccess) {
                println("✅ [KvcConnector] Показание успешно передано")
            }

            result
        } catch (e: Exception) {
            println("❌ [KvcConnector] Ошибка: ${e.message}")
            Result.failure(e)
        }
    }


    override suspend fun getTransmissionPeriod(
        accountNumber: String,
        regionId: Int?
    ): Result<GetTransmissionPeriod.TransmissionPeriod> {
        if (regionId == null) {
            return Result.failure(Exception("Для КВЦ требуется указать регион"))
        }

        return try {
            // Загружаем конфигурации БД
            val locations = kvcRepository.getLocationsForRegion(regionId)
                .getOrElse { return Result.failure(it) }

            // Ищем абонента
            val abonentInfo = kvcRepository.getAbonentInfo(
                locations = locations,
                accountNumber = accountNumber,
                target = 0
            ).getOrElse { return Result.failure(it) }

            // Загружаем период
            val period = kvcRepository.getTransitDays(
                location = abonentInfo.location,
                accountNumber = accountNumber
            ).getOrElse { return Result.failure(it) }

            // Преобразуем через маппер
            Result.success(
                GetTransmissionPeriod.TransmissionPeriod(
                    startDay = KvcPeriodMapper.getStartDay(period),
                    endDay = KvcPeriodMapper.getEndDay(period)
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
