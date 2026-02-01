package ru.dr.meterreadings.data.repository.providers.kvc

import ru.dr.meterreadings.data.mappers.KvcPeriodMapper
import ru.dr.meterreadings.data.remote.dto.kvc.KvcMetersDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcMeterHistoryDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcLocationDto
import ru.dr.meterreadings.domain.connector.GetMeterHistory
import ru.dr.meterreadings.domain.connector.GetTransmissionPeriod
import ru.dr.meterreadings.domain.connector.GetRegions
import ru.dr.meterreadings.domain.connector.GetMeters
import ru.dr.meterreadings.domain.connector.ProviderConnector
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.domain.connector.SubmitReadings
import ru.dr.meterreadings.domain.connector.ValidateReading
import ru.dr.meterreadings.domain.constants.ProviderIds
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.mapNotNull

@Singleton
class KvcConnector @Inject constructor(
    private val kvcRepository: KvcRepository
) : ProviderConnector,
    GetRegions,
    GetAccounts,
    SubmitReadings,
    GetTransmissionPeriod,
    GetMeters,
    ValidateReading,
    GetMeterHistory {

    override val providerId: Long = ProviderIds.KVC
    override val providerName: String = "КВЦ"

    /**
     * Получить список регионов КВЦ
     */
    override suspend fun getRegions(): Result<List<GetRegions.RegionInfo>> {
        println("🌐 [KvcConnector] Запрос регионов...")
        val result = kvcRepository.getRegions()
        return result.fold(
            onSuccess = { regions ->
                println("✅ [KvcConnector] Получено регионов: ${regions.size}")
                val regionInfoList = regions.map { region ->
                    GetRegions.RegionInfo(
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
     * получить аккаунт
     */
    override suspend fun getAccounts(
        accountNumber: String,
        regionId: String?
    ): Result<List<GetAccounts.AccountInfo>> {
        println("🔍 [KvcConnector] Получение ЛС $accountNumber в регионе $regionId")
        return try {
            val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                "Для КВЦ необходимо указать регион"
            }

            val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                .getOrElse { return Result.failure(it) }

            kvcRepository.getAccount(
                locations = locations,
                accountNumber = accountNumber,
                target = 0
            ).map {
                listOf(GetAccounts.AccountInfo(accountNumber = accountNumber))
            }
        } catch (e: Exception) {
            println("❌ [KvcConnector] Ошибка: ${e.message}")
            Result.failure(e)
        }
    }


    /**
     * Загрузка счётчиков для аккаунта КВЦ
     */
    override suspend fun getMeters(
        accountNumber: String,
        regionId: String?
    ): Result<GetMeters.GetMetersResult> {
        return try {
            val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                "Для КВЦ необходимо указать регион"
            }

            println("🔍 [KvcConnector] Загрузка счётчиков для ЛС $accountNumber")

            // ШАГ 1: Конфигурации БД
            val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                .getOrElse { return Result.failure(it) }

            // ШАГ 2: Ищем абонента
            val abonentInfo = kvcRepository.getAccount(
                locations = locations,
                accountNumber = accountNumber,
                target = 0
            ).getOrElse { return Result.failure(it) }

            val address = abonentInfo.getFullAddress()

            // ШАГ 3: Получаем счётчики
            val kvcCounters = kvcRepository.getMeters(
                location = abonentInfo.location,
                accountNumber = accountNumber
            ).getOrElse { return Result.failure(it) }

            // ШАГ 4: Маппинг в универсальный формат
            val meters = kvcCounters
                .filter { it.canEdit() }
                .map { counter ->
                    GetMeters.MeterInfo(
                        id = counter.idCnt.toString(),
                        type = if (counter.idTtype == "2T") {
                            "${counter.servName.trim()} (${counter.idTtype})"
                        } else {
                            counter.servName.trim()
                        },
                        serialNumber = counter.number.trim(),
                        lastValue = parseValueAsInt(counter.cValLst),
                        lastSubmissionDate = counter.datB.trim().takeIf { it.isNotBlank() },
                        apiCounterId = counter.idCnt
                    )
                }

            println("✅ [KvcConnector] Загружено ${meters.size} счётчиков")

            Result.success(
                GetMeters.GetMetersResult(
                    meters = meters,
                    //address = address,
                    cacheData = mapOf<String, Any>(
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

    /**
     * Получить историю показаний счётчика
     */
    override suspend fun getMeterHistory(
        counterId: String,
        accountNumber: String,
        regionId: String?,
        cacheData: Any?
    ): Result<List<GetMeterHistory.MeterHistory>> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val cache = cacheData as? Map<String, Any>
            val location = cache?.get("location") as? KvcLocationDto

            val actualLocation = if (location != null) {
                location
            } else {
                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                    "Для КВЦ необходимо указать регион"
                }

                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }

                val abonentInfo = kvcRepository.getAccount(
                    locations = locations,
                    accountNumber = accountNumber,
                    target = 0
                ).getOrElse { return Result.failure(it) }

                abonentInfo.location
            }

            // ✅ Загружаем историю (это Result!)
            val historyResult = kvcRepository.getMeterHistory(
                location = actualLocation,
                accountNumber = accountNumber,
                meterId = counterId.toInt()
            )

            // ✅ Извлекаем List из Result
            val history: List<KvcMeterHistoryDto> = historyResult.getOrElse {
                return Result.failure(it)
            }

            // ✅ Теперь можно вызвать mapNotNull
            val entries = history.mapNotNull { entry ->
                try {
                    val datePart = entry.datB.substringBefore("T")
                    val parts = datePart.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        GetMeterHistory.MeterHistory(
                            month = month,
                            year = year,
                            value = entry.valLst.toInt(),
                            consumption = entry.diff.toInt()
                        )
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получить минимально допустимое показание для валидации (КВЦ)
     *
     * Логика для КВЦ:
     * 1. Загружаем историю показаний через kvcRepository.getCounterHistory()
     * 2. Берём первую запись (последний период передачи)
     * 3. Определяем текущий месяц и год
     * 4. Сравниваем текущий месяц с месяцем из datB первой записи:
     *    - Если совпадают (показания за текущий месяц уже передавали):
     *      → Минимум = valPr (предыдущее показание данного периода)
     *    - Если не совпадают (еще не передавали в текущем месяце):
     *      → Минимум = valLst (показание переданное в данный период)
     *
     * Примечание:
     * - История возвращается НЕ преобразованной в HistoryEntry
     * - Работаем напрямую с List<KvcCounterHistoryDto>
     * - Если истории нет — возвращаем null (валидации не будет)
     *
     * @param counterId API ID счётчика (idCnt)
     * @param accountNumber Номер лицевого счёта
     * @param regionId ID региона (обязательно для КВЦ)
     * @param cacheData Кеш с location для оптимизации запросов
     * @return Result с минимальным значением или null
     */
    override suspend fun getMinimumAllowedValue(
        counterId: String,
        accountNumber: String,
        regionId: String?,
        cacheData: Any?
    ): Result<Int?> {
        return try {
            println("🔍 [KvcConnector] Получаем минимальное значение для счётчика $counterId")

            // ШАГ 1: Получаем location из кеша или загружаем
            @Suppress("UNCHECKED_CAST")
            val cache = cacheData as? Map<*, *>
            val location = cache?.get("location") as? KvcLocationDto

            val actualLocation = if (location != null) {
                println("✅ [KvcConnector] Используем location из кеша")
                location
            } else {
                println("⚠️ [KvcConnector] Location не найден в кеше, загружаем...")
                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                    "Для КВЦ необходимо указать регион"
                }

                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }

                val abonentInfo = kvcRepository.getAccount(
                    locations = locations,
                    accountNumber = accountNumber,
                    target = 0
                ).getOrElse { return Result.failure(it) }

                abonentInfo.location
            }

            // ШАГ 2: Загружаем СЫРУЮ историю (KvcCounterHistoryDto)
            val historyResult = kvcRepository.getMeterHistory(
                location = actualLocation,
                accountNumber = accountNumber,
                meterId = counterId.toInt()
            )

            val history: List<KvcMeterHistoryDto> = historyResult.getOrElse {
                println("❌ [KvcConnector] Не удалось загрузить историю: ${it.message}")
                return Result.failure(it)
            }

            // ШАГ 3: Проверяем, есть ли записи в истории
            if (history.isEmpty()) {
                println("⚠️ [KvcConnector] История пуста — валидации не будет")
                return Result.success(null)
            }

            // ШАГ 4: Берём первую запись (последний период передачи)
            val firstEntry = history.first()

            // ШАГ 5: Парсим дату из datB
            val datePart = firstEntry.datB.substringBefore("T") // "2026-01-01T00:00:00" → "2026-01-01"
            val parts = datePart.split("-")

            if (parts.size != 3) {
                println("❌ [KvcConnector] Неверный формат даты: ${firstEntry.datB}")
                return Result.failure(Exception("Неверный формат даты в истории"))
            }

            val entryYear = parts[0].toInt()
            val entryMonth = parts[1].toInt()

            // ШАГ 6: Определяем текущий месяц и год
            val now = java.util.Calendar.getInstance()
            val currentYear = now.get(java.util.Calendar.YEAR)
            val currentMonth = now.get(java.util.Calendar.MONTH) + 1 // Calendar.MONTH: 0-11

            // ШАГ 7: Логика выбора минимума
            val minValue = if (currentYear == entryYear && currentMonth == entryMonth) {
                // Совпадает: показания за текущий месяц уже передавали
                // Берём valPr (предыдущее показание данного периода)
                val value = firstEntry.valPr.toInt()
                println("✅ [KvcConnector] Текущий месяц = период истории ($currentMonth/$currentYear)")
                println("   Минимум = valPr = $value (предыдущее показание периода)")
                value
            } else {
                // Не совпадает: еще не передавали в текущем месяце
                // Берём valLst (показание переданное в данный период)
                val value = firstEntry.valLst.toInt()
                println("✅ [KvcConnector] Текущий месяц ($currentMonth/$currentYear) ≠ период истории ($entryMonth/$entryYear)")
                println("   Минимум = valLst = $value (показание переданное в период)")
                value
            }

            Result.success(minValue)

        } catch (e: Exception) {
            println("❌ [KvcConnector] Ошибка получения минимума: ${e.message}")
            e.printStackTrace()
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
        regionId: String?,
        cacheData: Any?
    ): Result<Unit> {
        return try {
            println("📤 [KvcConnector] Отправка показания: счётчик $counterId = $value")

            @Suppress("UNCHECKED_CAST")
            val cache = cacheData as? Map<String, Any>
            val location: KvcLocationDto
            val kvcCounters: List<KvcMetersDto>

            if (cache != null && cache.containsKey("location") && cache.containsKey("counters")) {
                println("✅ [KvcConnector] Используем кеш из ViewModel")
                location = cache["location"] as KvcLocationDto
                @Suppress("UNCHECKED_CAST")
                kvcCounters = cache["counters"] as List<KvcMetersDto>
            } else {
                println("⚠️ [KvcConnector] Кеш пуст, загружаем данные через API")
                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                    "Для КВЦ необходимо указать регион"
                }

                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }

                val abonentInfo = kvcRepository.getAccount(
                    locations = locations,
                    accountNumber = accountNumber,
                    target = 0
                ).getOrElse { return Result.failure(it) }

                location = abonentInfo.location

                kvcCounters = kvcRepository.getMeters(
                    location = location,
                    accountNumber = accountNumber
                ).getOrElse { return Result.failure(it) }
            }

            val counter = kvcCounters.firstOrNull { it.idCnt.toString() == counterId }
                ?: return Result.failure(Exception("Счётчик с ID $counterId не найден"))

            println("📋 [KvcConnector] Найден счётчик: ${counter.servName} №${counter.number}")

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
            val locations = kvcRepository.getLocationsForRegion(regionId)
                .getOrElse { return Result.failure(it) }

            val abonentInfo = kvcRepository.getAccount(
                locations = locations,
                accountNumber = accountNumber,
                target = 0
            ).getOrElse { return Result.failure(it) }

            val period = kvcRepository.getTransmissionPeriod(
                location = abonentInfo.location,
                accountNumber = accountNumber
            ).getOrElse { return Result.failure(it) }

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

    // Вспомогательные функции
    private fun parseValueAsInt(value: String): Int? {
        if (value.isBlank() || value == "0") return null
        return try {
            val cleaned = value.trim().replace(",", ".")
            cleaned.toDoubleOrNull()?.toInt()
        } catch (_: Exception) {
            null
        }
    }
}