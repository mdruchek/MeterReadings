package ru.dr.meterreadings.data.repository.providers.kvc

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
import ru.dr.meterreadings.utils.safeKvcCall
import ru.dr.meterreadings.utils.safeKvcCallNoAuth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Коннектор для провайдера КВЦ
 * ✅ Интегрирован с CaptchaService
 * ✅ Использует новый API репозитория (соответствует HAR)
 */
@Singleton
class KvcConnector @Inject constructor(
    private val repository: KvcRepository,
    private val captchaService: CaptchaService
) : ProviderConnector,
    GetRegions,
    GetAccounts
//    SubmitReadings,
//    GetMeters,
//    ValidateReading,
//    GetMeterHistory
    {

    override val providerId: Long = ProviderIds.KVC
    override val providerName: String = "КВЦ"

    // ==================== РЕГИОНЫ ====================
    override suspend fun getRegions(): Result<List<GetRegions.RegionInfo>> {
        println("KvcConnector: getRegions()")

        return safeKvcCallNoAuth {
            val regions = repository.getRegions().getOrThrow()
            regions.map { region ->
                GetRegions.RegionInfo(
                    id = region.id.toString(),
                    name = region.name
                )
            }
        }
    }

    // ==================== АККАУНТЫ ====================
    override suspend fun getAccounts(
        accountNumber: String,
        regionId: String?,
        login: String?
    ): Result<List<GetAccounts.AccountInfo>> {
        println("KvcConnector: getAccounts($accountNumber, regionId=$regionId)")

        val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
            "Region ID обязателен для КВЦ"
        }

        // ✅ ИСПОЛЬЗУЕМ ОБЁРТКУ
        return safeKvcCall(
            captchaService = captchaService,
            providerId = providerId,
            accountNumber = accountNumber
        ) { session ->  // ✅ Изменили с captchaToken на session
            val accountInfo = repository.getAccount(
                accountNumber = accountNumber,
                regionId = regionIdInt,
                session = session  // ✅ Передаём всю сессию
            )

            listOf(
                accountInfo
            )
        }
    }
//
//    // ==================== СЧЁТЧИКИ ====================
//    override suspend fun getMeters(
//        accountNumber: String,
//        regionId: String?
//    ): Result<GetMeters.GetMetersResult> {
//        val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
//            "Region ID обязателен для КВЦ"
//        }
//
//        println("KvcConnector: getMeters($accountNumber)")
//
//        // 1. Получаем locations (без капчи)
//        val locations = safeKvcCallNoAuth {
//            kvcRepository.getLocationsForRegion(regionIdInt).getOrThrow()
//        }.getOrElse { return Result.failure(it) }
//
//        if (locations.isEmpty()) {
//            return Result.failure(Exception("Не найдены населённые пункты для региона"))
//        }
//
//        val location = locations.first()
//
//        // 2. Получаем abonent (с капчей через обёртку)
//        val abonentInfo = safeKvcCall(
//            captchaService = captchaService,
//            providerId = providerId,
//            accountNumber = accountNumber
//        ) { captchaToken ->
//            kvcRepository.getAccount(
//                accountNumber = accountNumber,
//                regionId = regionIdInt,
//                captchaToken = captchaToken
//            ).getOrThrow()
//        }.getOrElse { return Result.failure(it) }
//
//        val abonentId = abonentInfo.id
//        println("KvcConnector: abonentId=$abonentId")
//
//        // 3. Получаем счётчики (без капчи)
//        val kvcCounters = safeKvcCallNoAuth {
//            kvcRepository.getMeters(abonentId = abonentId).getOrThrow()
//        }.getOrElse { return Result.failure(it) }
//
//        println("KvcConnector: Найдено счётчиков: ${kvcCounters.size}")
//
//        // 4. Фильтруем и мапим
//        val meters = kvcCounters
//            .filter { it.canEdit() }
//            .map { counter ->
//                GetMeters.MeterInfo(
//                    id = counter.idCnt.toString(),
//                    type = if (counter.idTtype == "2T") {
//                        "${counter.servName.trim()} (${counter.idTtype})"
//                    } else {
//                        counter.servName.trim()
//                    },
//                    serialNumber = counter.number.trim(),
//                    lastValue = parseValueAsInt(counter.cValLst),
//                    lastSubmissionDate = counter.datB.trim().takeIf { it.isNotBlank() },
//                    apiCounterId = counter.idCnt
//                )
//            }
//
//        println("KvcConnector: Доступно для передачи: ${meters.size}")
//
//        return Result.success(
//            GetMeters.GetMetersResult(
//                meters = meters,
//                cacheData = mapOf(
//                    "location" to location,
//                    "abonentId" to abonentId,
//                    "counters" to kvcCounters
//                )
//            )
//        )
//    }
//
//    // ==================== ИСТОРИЯ ====================
//    override suspend fun getMeterHistory(
//        counterId: String,
//        accountNumber: String,
//        regionId: String?,
//        cacheData: Any?
//    ): Result<List<GetMeterHistory.MeterHistory>> {
//        println("KvcConnector: getMeterHistory($counterId)")
//
//        // ✅ ОБОРАЧИВАЕМ В safeKvcCallNoAuth (история не требует капчу)
//        return safeKvcCallNoAuth {
//            // ============================================
//            // 1️⃣ ПОЛУЧЕНИЕ LOCATION (из cache или API)
//            // ============================================
//            @Suppress("UNCHECKED_CAST")
//            val cache = cacheData as? Map<String, Any>
//            val location = cache?.get("location") as? KvcLocationDto
//
//            val actualLocation = if (location != null) {
//                println("KvcConnector: Используем location из cache")
//                location
//            } else {
//                println("KvcConnector: Location нет в cache, запрашиваем...")
//                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
//                    "Region ID обязателен"
//                }
//
//                // ⚠️ Вложенный safeKvcCallNoAuth (или просто .getOrThrow())
//                val locations = kvcRepository.getLocationsForRegion(regionIdInt).getOrThrow()
//
//                locations.firstOrNull()
//                    ?: throw Exception("Не найдены населённые пункты")
//            }
//
//            // ============================================
//            // 2️⃣ ПОЛУЧЕНИЕ ИСТОРИИ
//            // ============================================
//            val history = kvcRepository.getMeterHistory(
//                location = actualLocation,
//                accountNumber = accountNumber,
//                meterId = counterId.toInt()
//            ).getOrThrow() // ✅ Вместо .getOrElse { return ... }
//
//            // ============================================
//            // 3️⃣ МАППИНГ ДАННЫХ
//            // ============================================
//            history.mapNotNull { entry ->
//                try {
//                    val datePart = entry.datB.substringBefore("T")
//                    val parts = datePart.split("-")
//
//                    if (parts.size == 3) {
//                        val year = parts[0].toInt()
//                        val month = parts[1].toInt()
//
//                        GetMeterHistory.MeterHistory(
//                            month = month,
//                            year = year,
//                            value = entry.valLst.toInt(),
//                            consumption = entry.diff.toInt()
//                        )
//                    } else null
//                } catch (e: Exception) {
//                    println("⚠️ [KvcConnector] Ошибка парсинга entry: ${e.message}")
//                    null
//                }
//            }
//        }
//    }
//
//    // ==================== МИНИМАЛЬНОЕ ЗНАЧЕНИЕ ====================
//    override suspend fun getMinimumAllowedValue(
//        counterId: String,
//        accountNumber: String,
//        regionId: String?,
//        cacheData: Any?
//    ): Result<Int?> {
//        println("KvcConnector: getMinimumAllowedValue($counterId)")
//
//        // ✅ ОБОРАЧИВАЕМ В safeKvcCallNoAuth (история не требует капчу)
//        return safeKvcCallNoAuth {
//            // ============================================
//            // 1️⃣ ПОЛУЧЕНИЕ LOCATION
//            // ============================================
//            @Suppress("UNCHECKED_CAST")
//            val cache = cacheData as? Map<*, *>
//            val location = cache?.get("location") as? KvcLocationDto
//
//            val actualLocation = if (location != null) {
//                println("KvcConnector: Используем location из cache")
//                location
//            } else {
//                println("KvcConnector: Location нет в cache, запрашиваем...")
//                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
//                    "Region ID обязателен"
//                }
//
//                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
//                    .getOrThrow() // ✅ Бросаем исключение (ловится обёрткой)
//
//                locations.firstOrNull()
//                    ?: throw Exception("Не найдены населённые пункты") // ✅ Бросаем
//            }
//
//            // ============================================
//            // 2️⃣ ПОЛУЧЕНИЕ ИСТОРИИ
//            // ============================================
//            val history = kvcRepository.getMeterHistory(
//                location = actualLocation,
//                accountNumber = accountNumber,
//                meterId = counterId.toInt()
//            ).getOrThrow() // ✅ Бросаем исключение вместо getOrElse
//
//            // ============================================
//            // 3️⃣ ПРОВЕРКА ПУСТОЙ ИСТОРИИ
//            // ============================================
//            if (history.isEmpty()) {
//                println("KvcConnector: История пуста, минимум не определён")
//                return@safeKvcCallNoAuth null // ✅ Возвращаем null (обернётся в Result.success(null))
//            }
//
//            // ============================================
//            // 4️⃣ ПАРСИНГ ДАТЫ
//            // ============================================
//            val firstEntry = history.first()
//            val datePart = firstEntry.datB.substringBefore("T")
//            val parts = datePart.split("-")
//
//            require(parts.size == 3) { // ✅ require бросает IllegalArgumentException
//                "Неверный формат даты: ${firstEntry.datB}"
//            }
//
//            val entryYear = parts[0].toInt()
//            val entryMonth = parts[1].toInt()
//
//            // ============================================
//            // 5️⃣ ОПРЕДЕЛЕНИЕ МИНИМУМА
//            // ============================================
//            val now = java.util.Calendar.getInstance()
//            val currentYear = now.get(java.util.Calendar.YEAR)
//            val currentMonth = now.get(java.util.Calendar.MONTH) + 1
//
//            val minValue = if (currentYear == entryYear && currentMonth == entryMonth) {
//                // Текущий месяц - берём valPr
//                val value = firstEntry.valPr.toInt()
//                println("KvcConnector: Текущий месяц ($currentMonth/$currentYear)")
//                println("KvcConnector: Минимум = valPr = $value")
//                value
//            } else {
//                // Другой месяц - берём valLst
//                val value = firstEntry.valLst.toInt()
//                println("KvcConnector: Прошлый месяц ($entryMonth/$entryYear)")
//                println("KvcConnector: Минимум = valLst = $value")
//                value
//            }
//
//            minValue // ✅ Возвращаем напрямую (обернётся в Result.success)
//        }
//    }
//
//    // ==================== ОТПРАВКА ПОКАЗАНИЙ ====================
//    override suspend fun submitReading(
//        counterId: String,
//        accountNumber: String,
//        value: String,
//        valueNight: String?,
//        regionId: String?,
//        cacheData: Any?
//    ): Result<Unit> {
//        println("KvcConnector: submitReading($counterId, value=$value)")
//
//        // ✅ ОБОРАЧИВАЕМ В safeKvcCallNoAuth (submitReading не требует капчу)
//        return safeKvcCallNoAuth {
//            // ============================================
//            // 1️⃣ ПОЛУЧЕНИЕ LOCATION И COUNTERS
//            // ============================================
//            @Suppress("UNCHECKED_CAST")
//            val cache = cacheData as? Map<String, Any>
//            val location: KvcLocationDto
//            val kvcCounters: List<KvcMetersDto>
//
//            if (cache != null && cache.containsKey("location") && cache.containsKey("counters")) {
//                // ---- ИЗ КЕША ----
//                println("KvcConnector: Используем данные из ViewModel cache")
//                location = cache["location"] as KvcLocationDto
//                @Suppress("UNCHECKED_CAST")
//                kvcCounters = cache["counters"] as List<KvcMetersDto>
//
//            } else {
//                // ---- ИЗ API ----
//                println("KvcConnector: Cache пустой, запрашиваем данные через API")
//                val regionIdInt = requireNotNull(regionId?.toIntOrNull()) {
//                    "Region ID обязателен"
//                }
//
//                // 1.1 Получаем locations (без капчи)
//                val locations = kvcRepository.getLocationsForRegion(regionIdInt)
//                    .getOrThrow() // ✅ Бросаем исключение
//
//                location = locations.firstOrNull()
//                    ?: throw Exception("Не найдены населённые пункты") // ✅ Бросаем
//
//                // 1.2 Получаем abonentInfo (С КАПЧЕЙ через вложенный safeKvcCall)
//                val abonentInfo = safeKvcCall(
//                    captchaService = captchaService,
//                    providerId = providerId,
//                    accountNumber = accountNumber
//                ) { captchaToken ->
//                    kvcRepository.getAccount(
//                        accountNumber = accountNumber,
//                        regionId = regionIdInt,
//                        captchaToken = captchaToken
//                    ).getOrThrow()
//                }.getOrThrow() // ✅ Разворачиваем Result и бросаем при ошибке
//
//                // 1.3 Получаем счётчики (без капчи)
//                kvcCounters = kvcRepository.getMeters(abonentId = abonentInfo.id)
//                    .getOrThrow() // ✅ Бросаем исключение
//            }
//
//            // ============================================
//            // 2️⃣ ПОИСК СЧЁТЧИКА
//            // ============================================
//            val counter = kvcCounters.firstOrNull { it.idCnt.toString() == counterId }
//                ?: throw Exception("Счётчик с ID $counterId не найден") // ✅ Бросаем
//
//            println("KvcConnector: Отправка для счётчика: ${counter.servName} №${counter.number}")
//
//            // ============================================
//            // 3️⃣ ОТПРАВКА ПОКАЗАНИЙ
//            // ============================================
//            kvcRepository.submitReading(
//                counter = counter,
//                location = location,
//                value = value,
//                valueNight = valueNight
//            ).getOrThrow() // ✅ Бросаем исключение при ошибке
//
//            println("KvcConnector: ✅ Показания успешно отправлены")
//
//            // ✅ Возвращаем Unit (обернётся в Result.success(Unit))
//        }
//    }
//
//    // ==================== УТИЛИТЫ ====================
//
//    private fun parseValueAsInt(value: String): Int? {
//        if (value.isBlank() || value == "0") return null
//        return try {
//            val cleaned = value.trim().replace(",", ".")
//            cleaned.toDoubleOrNull()?.toInt()
//        } catch (e: Exception) {
//            null
//        }
//    }
}
