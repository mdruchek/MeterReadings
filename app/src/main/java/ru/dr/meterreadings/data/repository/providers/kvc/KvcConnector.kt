package ru.dr.meterreadings.data.repository.providers.kvc

import ru.dr.meterreadings.data.mappers.KvcPeriodMapper
import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException
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
import ru.dr.meterreadings.domain.service.CaptchaService
import ru.dr.meterreadings.domain.service.CaptchaResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Коннектор для провайдера КВЦ
 * ✅ Интегрирован с CaptchaService
 * ✅ Использует новый API репозитория (соответствует HAR)
 */
@Singleton
class KvcConnector @Inject constructor(
    private val kvcRepository: KvcRepository,
    private val captchaService: CaptchaService
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

    // ==================== РЕГИОНЫ ====================

    override suspend fun getRegions(): Result<List<GetRegions.RegionInfo>> {
        println("KvcConnector: getRegions()")

        val result = kvcRepository.getRegions()

        return result.fold(
            onSuccess = { regions ->
                println("KvcConnector: Получено регионов: ${regions.size}")
                val regionInfoList = regions.map { region ->
                    GetRegions.RegionInfo(
                        id = region.id.toString(),
                        name = region.name
                    )
                }
                Result.success(regionInfoList)
            },
            onFailure = { error ->
                println("KvcConnector: Ошибка getRegions: ${error.message}")
                Result.failure(error)
            }
        )
    }

    // ==================== АККАУНТЫ ====================

    /**
     * ✅ ИСПРАВЛЕНО: интеграция с CaptchaService
     */
    override suspend fun getAccounts(
        accountNumber: String,
        regionId: String?,
        login: String?
    ): Result<List<GetAccounts.AccountInfo>> {
        println("KvcConnector: getAccounts($accountNumber, regionId=$regionId)")

        return try {
            val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                "Region ID обязателен для КВЦ"
            }

            // 1. Проверяем наличие токена капчи
            val captchaResult = captchaService.getValidCaptchaToken(
                providerId = providerId,
                accountNumber = accountNumber
            )

            val captchaToken = when (captchaResult) {
                is CaptchaResult.Success -> captchaResult.token
                is CaptchaResult.ShowCaptcha -> {
                    // Нет токена - нужно показать капчу
                    println("KvcConnector: Требуется капча для $accountNumber")
                    return Result.failure(
                        CaptchaRequiredException("Требуется пройти проверку капчи")
                    )
                }
            }

            println("KvcConnector: Используем сохранённый токен капчи")

            // 2. Вызываем API с токеном капчи
            val accountResult = kvcRepository.getAccount(
                accountNumber = accountNumber,
                regionId = regionIdInt,
                captchaToken = captchaToken
            )

            accountResult.fold(
                onSuccess = { accountInfo ->
                    println("KvcConnector: ✅ Аккаунт найден: ${accountInfo.account}")

                    // 3. Токен валидный - сохраняем его (на случай если был новый)
                    captchaService.saveCaptchaToken(
                        providerId = providerId,
                        accountNumber = accountNumber,
                        captchaToken = captchaToken
                    )

                    Result.success(
                        listOf(
                            GetAccounts.AccountInfo(
                                accountNumber = accountInfo.account
                            )
                        )
                    )
                },
                onFailure = { error ->
                    // 4. Обработка ошибки капчи
                    if (error is CaptchaRequiredException) {
                        println("KvcConnector: ❌ Ошибка капчи - инвалидируем токен")
                        captchaService.invalidateCaptchaToken(
                            providerId = providerId,
                            accountNumber = accountNumber
                        )
                    }

                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("KvcConnector: Ошибка getAccounts: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== СЧЁТЧИКИ ====================

    /**
     * ✅ ИСПРАВЛЕНО: новый API репозитория
     * - getAccount() возвращает ID (UUID)
     * - getMeters(abonentId) вместо getMeters(location, account)
     * - Сохраняем location в cacheData для submitReading
     */
    override suspend fun getMeters(
        accountNumber: String,
        regionId: String?
    ): Result<GetMeters.GetMetersResult> {
        return try {
            val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
                "Region ID обязателен для КВЦ"
            }

            println("KvcConnector: getMeters($accountNumber)")

            // 1. Получаем locations (нужны для submitReading)
            val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                .getOrElse { return Result.failure(it) }

            if (locations.isEmpty()) {
                return Result.failure(Exception("Не найдены населённые пункты для региона"))
            }

            val location = locations.first()

            // 2. Проверяем токен капчи
            val captchaResult = captchaService.getValidCaptchaToken(
                providerId = providerId,
                accountNumber = accountNumber
            )

            val captchaToken = when (captchaResult) {
                is CaptchaResult.Success -> captchaResult.token
                is CaptchaResult.ShowCaptcha -> {
                    println("KvcConnector: Требуется капча для getMeters")
                    return Result.failure(
                        CaptchaRequiredException("Требуется пройти проверку капчи")
                    )
                }
            }

            // 3. Получаем информацию о аккаунте (для abonentId)
            val abonentInfo = kvcRepository.getAccount(
                accountNumber = accountNumber,
                regionId = regionIdInt,
                captchaToken = captchaToken
            ).getOrElse { error ->
                if (error is CaptchaRequiredException) {
                    captchaService.invalidateCaptchaToken(providerId, accountNumber)
                }
                return Result.failure(error)
            }

            val address = abonentInfo.getFullAddress()
            val abonentId = abonentInfo.id

            println("KvcConnector: abonentId=$abonentId")

            // 4. Получаем счётчики по abonentId (новый API!)
            val kvcCounters = kvcRepository.getMeters(abonentId = abonentId)
                .getOrElse { return Result.failure(it) }

            println("KvcConnector: Найдено счётчиков: ${kvcCounters.size}")

            // 5. Фильтруем и мапим счётчики
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

            println("KvcConnector: Доступно для передачи: ${meters.size}")

            // 6. Сохраняем данные для submitReading
            Result.success(
                GetMeters.GetMetersResult(
                    meters = meters,
                    //address = address,
                    cacheData = mapOf(
                        "location" to location,          // ← Для submitReading
                        "abonentId" to abonentId,        // ← Для других запросов
                        "counters" to kvcCounters        // ← Для submitReading
                    )
                )
            )
        } catch (e: Exception) {
            println("KvcConnector: Ошибка getMeters: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== ИСТОРИЯ ====================

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

            // Если location есть в кеше - используем его
            val actualLocation = if (location != null) {
                location
            } else {
                // Иначе запрашиваем заново
                val regionIdInt = requireNotNull(regionId?.toIntOrNull())
                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }
                locations.firstOrNull() ?: return Result.failure(
                    Exception("Не найдены населённые пункты")
                )
            }

            val historyResult = kvcRepository.getMeterHistory(
                location = actualLocation,
                accountNumber = accountNumber,
                meterId = counterId.toInt()
            )

            val history = historyResult.getOrElse { return Result.failure(it) }

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
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== МИНИМАЛЬНОЕ ЗНАЧЕНИЕ ====================

    override suspend fun getMinimumAllowedValue(
        counterId: String,
        accountNumber: String,
        regionId: String?,
        cacheData: Any?
    ): Result<Int?> {
        return try {
            println("KvcConnector: getMinimumAllowedValue($counterId)")

            @Suppress("UNCHECKED_CAST")
            val cache = cacheData as? Map<*, *>
            val location = cache?.get("location") as? KvcLocationDto

            val actualLocation = if (location != null) {
                println("KvcConnector: Используем location из cache")
                location
            } else {
                println("KvcConnector: Location нет в cache, запрашиваем...")
                val regionIdInt = requireNotNull(regionId?.toIntOrNull())
                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }
                locations.firstOrNull() ?: return Result.failure(
                    Exception("Не найдены населённые пункты")
                )
            }

            val historyResult = kvcRepository.getMeterHistory(
                location = actualLocation,
                accountNumber = accountNumber,
                meterId = counterId.toInt()
            )

            val history = historyResult.getOrElse {
                println("KvcConnector: Ошибка getMeterHistory: ${it.message}")
                return Result.failure(it)
            }

            if (history.isEmpty()) {
                println("KvcConnector: История пуста, минимум не определён")
                return Result.success(null)
            }

            val firstEntry = history.first()

            // Парсим дату
            val datePart = firstEntry.datB.substringBefore("T")
            val parts = datePart.split("-")

            if (parts.size != 3) {
                println("KvcConnector: Неверный формат даты: ${firstEntry.datB}")
                return Result.failure(Exception("Неверный формат даты"))
            }

            val entryYear = parts[0].toInt()
            val entryMonth = parts[1].toInt()

            // Текущая дата
            val now = java.util.Calendar.getInstance()
            val currentYear = now.get(java.util.Calendar.YEAR)
            val currentMonth = now.get(java.util.Calendar.MONTH) + 1

            // Логика выбора минимума
            val minValue = if (currentYear == entryYear && currentMonth == entryMonth) {
                // Текущий месяц - берём valPr
                val value = firstEntry.valPr.toInt()
                println("KvcConnector: Текущий месяц ($currentMonth/$currentYear)")
                println("KvcConnector: Минимум = valPr = $value")
                value
            } else {
                // Другой месяц - берём valLst
                val value = firstEntry.valLst.toInt()
                println("KvcConnector: Прошлый месяц ($entryMonth/$entryYear)")
                println("KvcConnector: Минимум = valLst = $value")
                value
            }

            Result.success(minValue)
        } catch (e: Exception) {
            println("KvcConnector: Ошибка getMinimumAllowedValue: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ==================== ОТПРАВКА ПОКАЗАНИЙ ====================

    override suspend fun submitReading(
        counterId: String,
        accountNumber: String,
        value: String,
        valueNight: String?,
        regionId: String?,
        cacheData: Any?
    ): Result<Unit> {
        return try {
            println("KvcConnector: submitReading($counterId, value=$value)")

            @Suppress("UNCHECKED_CAST")
            val cache = cacheData as? Map<String, Any>
            val location: KvcLocationDto
            val kvcCounters: List<KvcMetersDto>

            if (cache != null && cache.containsKey("location") && cache.containsKey("counters")) {
                println("KvcConnector: Используем данные из ViewModel cache")
                location = cache["location"] as KvcLocationDto
                @Suppress("UNCHECKED_CAST")
                kvcCounters = cache["counters"] as List<KvcMetersDto>
            } else {
                println("KvcConnector: Cache пустой, запрашиваем данные через API")
                val regionIdInt = requireNotNull(regionId?.toIntOrNull())

                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                    .getOrElse { return Result.failure(it) }
                location = locations.firstOrNull() ?: return Result.failure(
                    Exception("Не найдены населённые пункты")
                )

                // Нужен captchaToken для getAccount
                val captchaResult = captchaService.getValidCaptchaToken(providerId, accountNumber)
                val captchaToken = when (captchaResult) {
                    is CaptchaResult.Success -> captchaResult.token
                    is CaptchaResult.ShowCaptcha -> {
                        return Result.failure(
                            CaptchaRequiredException("Требуется пройти проверку капчи")
                        )
                    }
                }

                val abonentInfo = kvcRepository.getAccount(
                    accountNumber = accountNumber,
                    regionId = regionIdInt,
                    captchaToken = captchaToken
                ).getOrElse {return Result.failure(it) }

                kvcCounters = kvcRepository.getMeters(abonentId = abonentInfo.id)
                    .getOrElse { return Result.failure(it) }
            }

            val counter = kvcCounters.firstOrNull { it.idCnt.toString() == counterId }
                ?: return Result.failure(Exception("Счётчик с ID $counterId не найден"))

            println("KvcConnector: Отправка для счётчика: ${counter.servName} №${counter.number}")

            val result = kvcRepository.submitReading(
                counter = counter,
                location = location,
                value = value,
                valueNight = valueNight
            )

            if (result.isSuccess) {
                println("KvcConnector: ✅ Показания успешно отправлены")
            }

            result
        } catch (e: Exception) {
            println("KvcConnector: Ошибка submitReading: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== ПЕРИОД ПЕРЕДАЧИ ====================

    override suspend fun getTransmissionPeriod(
        accountNumber: String,
        regionId: String?
    ): Result<GetTransmissionPeriod.TransmissionPeriod> {
        val regionIdInt = requireNotNull(regionId?.toIntOrNull())

        return try {
            val locations = kvcRepository.getLocationsForRegion(regionIdInt)
                .getOrElse { return Result.failure(it) }

            val location = locations.firstOrNull() ?: return Result.failure(
                Exception("Не найдены населённые пункты")
            )

            val period = kvcRepository.getTransmissionPeriod(
                location = location,
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

    // ==================== УТИЛИТЫ ====================

    private fun parseValueAsInt(value: String): Int? {
        if (value.isBlank() || value == "0") return null
        return try {
            val cleaned = value.trim().replace(",", ".")
            cleaned.toDoubleOrNull()?.toInt()
        } catch (e: Exception) {
            null
        }
    }
}
