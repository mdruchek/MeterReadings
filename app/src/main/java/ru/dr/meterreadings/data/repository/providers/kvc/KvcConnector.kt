package ru.dr.meterreadings.data.repository.providers.kvc

import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException
import ru.dr.meterreadings.domain.connector.GetRegions
import ru.dr.meterreadings.domain.connector.ProviderConnector
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.domain.connector.GetMeterHistory
import ru.dr.meterreadings.domain.connector.GetMeters
import ru.dr.meterreadings.domain.constants.ProviderIds
import ru.dr.meterreadings.domain.exceptions.AccountNotFoundException
import ru.dr.meterreadings.domain.exceptions.MeterNotFoundException
import ru.dr.meterreadings.domain.service.CaptchaService
import java.time.Year
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
    GetAccounts,
    GetMeters,
    GetMeterHistory
//    SubmitReadings,
//    ValidateReading,
    {

    override val providerId: Long = ProviderIds.KVC
    override val providerName: String = "КВЦ"

    // ==================== РЕГИОНЫ ====================

    override suspend fun getRegions(): Result<List<GetRegions.RegionInfo>> {
        println("🌐 [TnsConnector] Запрос регионов...")

        return repository.getRegions()
            .map { dtoList ->
                dtoList.map { dto ->
                    GetRegions.RegionInfo(
                        id = dto.id.toString(),
                        name = dto.name
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
            println("🌐 [KvcConnector] getAccounts($accountNumber, regionId=$regionId)")

            // ✅ Валидация regionId (возвращаем Result.failure вместо исключения)
                val regionIdInt = regionId?.toIntOrNull()
                    ?: return Result.failure(Exception("Region ID обязателен для КВЦ"))

            // ✅ Получаем сессию капчи
            val session = captchaService.getCaptchaSession(providerId, accountNumber)
                ?: return Result.failure(CaptchaRequiredException("Требуется пройти проверку капчи"))

            println("🔐 [KvcConnector] Используем сессию капчи")

            // ✅ Вызываем Repository
            return repository.getAccount(
                accountNumber = accountNumber,
                regionId = regionIdInt,
                session = session
            )
                .map { account ->  // ✅ Преобразуем DTO → Interface Model
                    listOf(
                        GetAccounts.AccountInfo(
                            number = account.number,
                            uuid = account.id,
                            address = account.address,
                            regionId = regionId,
                            login = null,
                            submissionStartDay = account.submissionStartDay,
                            submissionEndDay = account.submissionEndDay,
                            additionalInfo = null
                        )
                    )
                }
                .onFailure { error ->  // ✅ Обрабатываем ошибки капчи
                    if (error is CaptchaRequiredException) {
                        println("🗑️ [KvcConnector] Капча невалидна - очищаем сессию")
                        captchaService.clearCaptchaSession(providerId, accountNumber)
                    }
                }
        }

        // ==================== СЧЁТЧИКИ ====================
        /**
         * Получить список счётчиков для аккаунта
         *
         * ✅ Для КВЦ требуется UUID аккаунта (apiAccountId)
         * ✅ UUID передаётся через параметр apiAccountId
         *
         *
         * @param accountNumber Номер лицевого счёта (НЕ используется в КВЦ API)
         * @param regionId ID региона (НЕ используется в КВЦ API)
         * @param apiAccountId UUID аккаунта (apiAccountId) - ОБЯЗАТЕЛЬНО для КВЦ
         * @return Result со списком счётчиков
         * @throws AccountNotFoundException если UUID устарел (400 от API)
         */
        override suspend fun getMeters(
            accountNumber: String,
            regionId: String?,
            apiAccountId: String?
        ): Result<GetMeters.GetMetersResult> {
            println("🔍 [KvcConnector] getMeters для ЛС=$accountNumber")

            return try {
                // ✅ Проверяем наличие UUID
                val notNullApiAccountId = apiAccountId
                    ?: return Result.failure(
                        IllegalArgumentException(
                            "Для КВЦ требуется UUID аккаунта. " +
                                    "Используйте: getMeters(accountNumber, regionId, apiAccountId = account.uuid)"
                        )
                    )

                println("   UUID аккаунта: $notNullApiAccountId")

                // ✅ Загружаем счётчики из API
                val metersResult = repository.getMeters(notNullApiAccountId)

                // ✅ Преобразуем DTO → MeterInfo
                metersResult.map { dtoListMeters ->
                    println("✅ [KvcConnector] Получено счётчиков: ${dtoListMeters.size}")

                    GetMeters.GetMetersResult(
                        meters = dtoListMeters.map { dtoMeter ->
                            println("   - ${dtoMeter.type}: ${dtoMeter.number}")
                            println("     T1=${dtoMeter.lastFirstValue}, T2=${dtoMeter.lastSecondValue}, T3=${dtoMeter.lastThirdValue}")

                            GetMeters.MeterInfo(
                                id = dtoMeter.id,  // ✅ UUID счётчика из API
                                number = dtoMeter.number,
                                lastFirstValue = dtoMeter.lastFirstValue.toInt(),
                                lastSecondValue = dtoMeter.lastSecondValue.toInt(),
                                lastThirdValue = dtoMeter.lastThirdValue.toInt(),
                                type = dtoMeter.type,
                                verificationDate = dtoMeter.verificationDate,
                                maxDiff = dtoMeter.maxDiff,
                                apiAccountId = notNullApiAccountId  // ✅ UUID аккаунта
                            )
                        }
                    )
                }
            } catch (e: AccountNotFoundException) {
                // ✅ UUID устарел → ViewModel должен обновить через getAccounts()
                println("❌ [KvcConnector] UUID устарел (ошибка 400 от API)")
                println("   Требуется обновление через getAccounts()")
                Result.failure(e)

            } catch (e: Exception) {
                println("❌ [KvcConnector] Ошибка загрузки счётчиков: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }


        // ==================== ИСТОРИЯ ====================
        /**
         * Получить историю передачи для счетчика
         *
         * ✅ Для КВЦ требуется id счетчика из api
         * ✅ id передаётся через параметр apiDeviceId
         *
         * @param meterId id счетчика из api
         * @param regionId ID региона (НЕ используется в КВЦ API)
         * @return Result со списком счётчиков
         * @throws AccountNotFoundException если UUID устарел (400 от API)
         */
        override suspend fun getMeterHistory(
            meterNumber: String?,
            meterId: String?,
            regionId: String?
        ): Result<List<GetMeterHistory.MeterHistoryInfo>> {
            println("🔍 [KvcConnector] getMeterHistory для счетчика id=$meterId")

            return try {
                
                val notNullMeterId = meterId
                    ?: return Result.failure(
                        IllegalArgumentException(
                            "Для КВЦ требуется id счетчика. " +
                                    "Используйте: getMeterHistory(meterId, regionId)"
                        )
                    )

                println("   id счетчика: $notNullMeterId")

                // ✅ Загружаем счётчики из API
                val meterHistoryResult = repository.getMeterHistory(notNullMeterId)

                // ✅ Преобразуем DTO → MeterHistoryInfo
                meterHistoryResult.map { dtoListMeterHistory ->
                    println("✅ [KvcConnector] Получено истории: ${dtoListMeterHistory.size}")

                    dtoListMeterHistory
                        .groupBy { it.submissionPeriod }
                        .map { ( period, tariffs ) ->
                            GetMeterHistory.MeterHistoryInfo(
                                month = period.split("-")[1].toInt(),
                                year = period.split("-")[0].toInt(),
                                tariffs = tariffs.map { tariff ->
                                    GetMeterHistory.TariffInfo(
                                        indicationType = tariff.indicationType,
                                        lastValue = tariff.lastValue.toInt(),
                                        prevValue = tariff.prevValue.toInt(),
                                        consumption = tariff.diff.toInt()
                                    )
                                }
                            )
                        }
                }
            } catch (e: MeterNotFoundException) {
                // ✅ UUID аккаунта и id счетчика устарел → ViewModel должен обновить через getAccounts() и getMeters()
                println("❌ [KvcConnector] UUID аккаунта и id счетчика устарел (ошибка 400 от API)")
                println("   Требуется обновление через getAccounts() и getMeters()")
                Result.failure(e)
            } catch (e: Exception) {
                println("❌ [KvcConnector] Ошибка загрузки счётчиков: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
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
