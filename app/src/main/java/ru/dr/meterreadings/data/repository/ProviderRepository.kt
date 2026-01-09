package ru.dr.meterreadings.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.remote.dto.KvcRegionDto
import ru.dr.meterreadings.data.local.dao.ProviderDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type
import ru.dr.meterreadings.data.remote.dto.CounterForInsertDto
import ru.dr.meterreadings.data.remote.dto.GetAbonentInfoRequest
import ru.dr.meterreadings.data.remote.dto.GetCntListRequest
import ru.dr.meterreadings.data.remote.dto.GetCtrDaysRequest
import ru.dr.meterreadings.data.remote.dto.InsertCtrRequest
import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
import ru.dr.meterreadings.data.remote.dto.KvcLocationDto
import ru.dr.meterreadings.data.remote.dto.KvcAbonentInfoDto
import ru.dr.meterreadings.data.remote.dto.KvcTransitDaysDto
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с провайдерами услуг
 */
@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val httpClient: HttpClient
) {

    // ========================================
    // ЧТЕНИЕ (READ)
    // ========================================

    /**
     * Получить все провайдеры как Flow (с автообновлением)
     */
    fun getAllProviders(): Flow<List<ProviderDomainModel>> {
        return providerDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Получить провайдера по ID
     */
    fun getProviderById(id: String): Flow<ProviderDomainModel?> {
        return providerDao.getById(id).map { it?.toDomain() }
    }

    /**
     * Получить провайдеров по типу услуги
     *
     * @param type Enum Type (WaterSupply, GasSupply, etc)
     */
    fun getProvidersByType(type: Type): Flow<List<ProviderDomainModel>> {
        // ✅ ИЗМЕНЕНИЕ: Конвертируем enum в строку для запроса к БД
        return providerDao.getAllByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Поиск провайдеров по названию
     */
    fun searchProviders(query: String): Flow<List<ProviderDomainModel>> {
        return providerDao.searchByName(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // ========================================
    // ЗАПИСЬ (WRITE)
    // ========================================

    /**
     * Добавить нового провайдера
     */
    suspend fun addProvider(provider: ProviderDomainModel) {
        val exists = providerDao.exists(provider.id)
        if (exists) {
            throw IllegalArgumentException("Provider with id '${provider.id}' already exists")
        }

        val entity = provider.toEntity()
        providerDao.insert(entity)
    }

    /**
     * Обновить провайдера
     */
    suspend fun updateProvider(provider: ProviderDomainModel) {
        val existingEntity = providerDao.getById(provider.id).first()
            ?: throw IllegalArgumentException("Provider not found")

        val updatedEntity = provider.toEntity(
            createdAt = existingEntity.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        providerDao.update(updatedEntity)
    }

    /**
     * Удалить провайдера
     */
    suspend fun deleteProvider(id: String) {
        providerDao.deleteById(id)
    }

    // ========================================
    // СПЕЦИАЛЬНЫЕ ОПЕРАЦИИ
    // ========================================

    /**
     * Получить количество провайдеров
     */
    suspend fun getProviderCount(): Int {
        return providerDao.getCount()
    }

    /**
     * Инициализировать БД моковыми провайдерами
     *
     * ✅ ИЗМЕНЕНИЕ: Используем новый enum Type
     */
    suspend fun initializeWithMockData() {
        val count = providerDao.getCount()
        if (count > 0) {
            println("⚠️ [ProviderRepository] БД уже содержит провайдеров: $count")
            return
        }

        println("✨ [ProviderRepository] Инициализация БД моковыми провайдерами")

        val mockProviders = listOf(
            // ✅ ИЗМЕНЕНИЕ: Используем Type.WaterSupply вместо строки
            ProviderDomainModel(
                id = "mosvodokanal",
                name = "Мосводоканал",
                type = Type.WaterSupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://mosvodokanal.me/",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "mosenergosby",
                name = "Мосэнергосбыт",
                type = Type.ElectricitySupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://mosenergosby.me/",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "mosoblgaz",
                name = "Мособлгаз",
                type = Type.GasSupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://mosoblgaz.me/",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "energosbyt",
                name = "Энергосбыт",
                type = Type.ElectricitySupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://energosbyt.ru",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "gazprom",
                name = "Газпром Межрегионгаз",
                type = Type.GasSupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://gazprom.ru",
                authType = AuthType.AUTH_REQUIRED
            )
        )

        val entities = mockProviders.map { it.toEntity() }
        providerDao.insertAll(entities)

        println("✅ [ProviderRepository] Добавлено ${mockProviders.size} провайдеров")
    }

    /**
     * Получить список регионов (районов) КВЦ
     *
     * Вызывается при добавлении счёта КВЦ,
     * до авторизации - для выбора района.
     */
    suspend fun getKvcRegions(): Result<List<KvcRegionDto>> {
        return try {
            val response = httpClient.post(
                urlString = "https://send.kvc-nn.ru/api/ControlIndications/GetActiveCtrRegions"
            )

            val regions = response.body<List<KvcRegionDto>>()

            println("✅ [Repository] Загружено регионов КВЦ: ${regions.size}")
            regions.forEach { region ->
                println("   📍 [${region.id}] ${region.name} (${region.codRs})")
            }

            Result.success(regions)
        } catch (e: Exception) {
            println("❌ [Repository] Ошибка загрузки регионов КВЦ: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Получить список конфигураций БД для региона КВЦ
     *
     * Это технические настройки подключения к базам данных провайдера.
     * Используется для последующих запросов (авторизация, получение данных).
     *
     * @param regionId ID региона (из getKvcRegions)
     * @return Список конфигураций БД для этого региона
     *
     * API: POST https://send.kvc-nn.ru/api/ControlIndications/GetLocationsForRegion?idRegion={regionId}
     * Response: [{"server": "DBASES03", "db_name": "co_vyksa", ...}, ...]
     */
    suspend fun getKvcLocationsForRegion(regionId: Int): Result<List<KvcLocationDto>> {
        return try {
            println("🔍 [Repository] Загружаем конфигурации БД для региона $regionId...")

            val response = httpClient.post(
                urlString = "https://send.kvc-nn.ru/api/ControlIndications/GetLocationsForRegion"
            ) {
                parameter("idRegion", regionId)
            }

            val locations = response.body<List<KvcLocationDto>>()

            println("✅ [Repository] Загружено конфигураций БД для региона $regionId: ${locations.size}")
            locations.forEach { location ->
                println("   💾 server=${location.server}, db=${location.dbName}")
            }

            Result.success(locations)
        } catch (e: Exception) {
            println("❌ [Repository] Ошибка загрузки конфигураций для региона $regionId: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить информацию об абоненте КВЦ по лицевому счёту
     *
     * Ищет абонента во всех БД региона по номеру лицевого счёта.
     *
     * @param locations Список конфигураций БД (из getKvcLocationsForRegion)
     * @param accountNumber Номер лицевого счёта
     * @param target Цель поиска (0 = поиск по всем БД)
     * @return Информация об абоненте
     */
    suspend fun getKvcAbonentInfo(
        locations: List<KvcLocationDto>,
        accountNumber: String,
        target: Int = 0
    ): Result<KvcAbonentInfoDto> {
        return try {
            println("🔍 [Repository] Получаем данные абонента: ЛС=$accountNumber, БД=${locations.size}...")

            val requestBody = GetAbonentInfoRequest(
                servDbs = locations,
                lc = accountNumber,
                target = target
            )

            val response = httpClient.post(
                urlString = "https://send.kvc-nn.ru/api/ControlIndications/GetAbonentInfo"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val abonentInfo = response.body<KvcAbonentInfoDto>()

            println("✅ [Repository] Получены данные абонента")
            println("   🆔 ID: ${abonentInfo.id}")
            println("   📋 ЛС: ${abonentInfo.lc.trim()}")
            println("   👤 ФИО: ${abonentInfo.fio ?: "Не указано"}")
            println("   🏠 Адрес: ${abonentInfo.getFullAddress()}")
            println("   💾 БД: ${abonentInfo.location.dbName}")

            Result.success(abonentInfo)
        } catch (e: Exception) {
            println("❌ [Repository] Ошибка получения данных абонента: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить список счётчиков абонента КВЦ
     *
     * Возвращает все счётчики (вода, газ, электричество) для конкретного лицевого счёта.
     *
     * @param location Конфигурация БД провайдера (из getKvcLocationsForRegion)
     * @param accountNumber Номер лицевого счёта
     * @return Список счётчиков абонента
     *
     * Пример:
     * ```
     * // Получили информацию об абоненте
     * val abonentInfo = getKvcAbonentInfo(...).getOrThrow()
     *
     * // Теперь получаем его счётчики
     * val counters = getKvcCounters(
     *     location = abonentInfo.location,
     *     accountNumber = abonentInfo.lc.trim()
     * ).getOrThrow()
     *
     * counters.forEach { counter ->
     *     println("${counter.servName}: ${counter.getLastReadingDay()}")
     * }
     * ```
     */
    suspend fun getKvcCounters(
        location: KvcLocationDto,
        accountNumber: String
    ): Result<List<KvcCounterDto>> {
        return try {
            println("🔍 [Repository] Получаем счётчики: ЛС=$accountNumber, БД=${location.dbName}")

            val requestBody = GetCntListRequest(
                servDb = location,
                lc = accountNumber
            )

            val response = httpClient.post(
                urlString = "https://send.kvc-nn.ru/api/ControlIndications/GetCntList"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val counters = response.body<List<KvcCounterDto>>()

            println("✅ [Repository] Получено счётчиков: ${counters.size}")
            counters.forEach { counter ->
                println("   📊 [${counter.idServ}] ${counter.servName.trim()}")
                println("      Номер: ${counter.number.trim()}")
                println("      Последнее: ${counter.getLastReadingDay()} (${counter.datLst})")
                counter.getLastReadingNight()?.let { night ->
                    println("      Ночной тариф: $night")
                }
            }

            Result.success(counters)
        } catch (e: Exception) {
            println("❌ [Repository] Ошибка получения счётчиков: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить разрешённые дни для передачи показаний КВЦ
     *
     * Возвращает диапазон дней месяца когда можно передавать показания.
     * Если текущий день не входит в диапазон - показания отправить нельзя.
     *
     * @param location Конфигурация БД провайдера (из getKvcAbonentInfo)
     * @param accountNumber Номер лицевого счёта
     * @return Диапазон разрешённых дней
     *
     * Пример:
     * ```
     * val transitDays = providerRepository.getKvcTransitDays(
     *     location = abonent.location,
     *     accountNumber = abonent.lc.trim()
     * ).getOrThrow()
     *
     * val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
     *
     * if (transitDays.canSubmitToday(today)) {
     *     println("✅ Можно передать показания (${transitDays.getRangeDescription()})")
     *     // Разрешаем отправку
     * } else {
     *     println("❌ Сейчас нельзя передать показания")
     *     println("Период: ${transitDays.getRangeDescription()}")
     * }
     * ```
     */
    suspend fun getKvcTransitDays(
        location: KvcLocationDto,
        accountNumber: String
    ): Result<KvcTransitDaysDto> {
        return try {
            println("🔍 [Repository] Получаем разрешённые дни передачи: ЛС=$accountNumber")

            val requestBody = GetCtrDaysRequest(
                servDb = location,
                lc = accountNumber
            )

            val response = httpClient.post(
                urlString = "https://send.kvc-nn.ru/api/ControlIndications/GetCtrDays"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val transitDays = response.body<KvcTransitDaysDto>()

            println("✅ [Repository] Получен диапазон дней: ${transitDays.first}-${transitDays.last}")

            // Проверяем текущий день
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            if (transitDays.canSubmitToday(today)) {
                val daysLeft = transitDays.daysUntilEnd(today)
                println("   ✅ Сегодня ($today) можно передать показания")
                daysLeft?.let { println("   ⏰ Осталось дней: $it") }
            } else {
                if (today < transitDays.first) {
                    val daysUntil = transitDays.daysUntilStart(today)
                    println("   ⏳ Ещё рано. До начала периода: $daysUntil дней")
                } else {
                    println("   ❌ Период передачи закончился")
                }
            }

            Result.success(transitDays)
        } catch (e: Exception) {
            println("❌ [Repository] Ошибка получения разрешённых дней: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Отправить показания счётчика КВЦ
     *
     * Передаёт новые показания на сервер КВЦ.
     * ВАЖНО: Перед отправкой обязательно проверяет разрешённый период передачи,
     * так как сервер не примет показания вне этого периода.
     *
     * @param counter Счётчик (из getKvcCounters)
     * @param location Конфигурация БД (из getKvcAbonentInfo)
     * @param value Значение показания (для однотарифного или дневной тариф)
     * @param valueNight Значение ночного тарифа (для двухтарифного, опционально)
     * @param notes Комментарий к передаче
     * @return Result<Unit> - успех или ошибка (включая ошибку "неразрешённый период")
     *
     * Пример:
     * ```
     * providerRepository.submitKvcReading(
     *     counter = counter,
     *     location = abonent.location,
     *     value = "900"
     * ).onSuccess {
     *     println("✅ Показания отправлены")
     * }.onFailure { error ->
     *     // Обработка ошибок (включая "вне периода передачи")
     *     println("❌ ${error.message}")
     * }
     * ```
     */
    suspend fun submitKvcReading(
        counter: KvcCounterDto,
        location: KvcLocationDto,
        value: String,
        valueNight: String? = null,
        notes: String = "Передано через приложение"
    ): Result<Unit> {
        return try {
            // ШАГ 1: Проверяем разрешённый период передачи
            println("🔍 [Repository] Проверяем разрешённый период передачи...")

            val transitDaysResult = getKvcTransitDays(
                location = location,
                accountNumber = counter.lc.trim()
            )

            if (transitDaysResult.isFailure) {
                return Result.failure(
                    Exception("Не удалось проверить разрешённые дни передачи")
                )
            }

            val transitDays = transitDaysResult.getOrThrow()
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            // Проверяем: входит ли текущий день в разрешённый период
            if (!transitDays.canSubmitToday(today)) {
                val message = if (today < transitDays.first) {
                    "Передача показаний доступна ${transitDays.getRangeDescription()}. " +
                            "До начала периода: ${transitDays.daysUntilStart(today)} дн."
                } else {
                    "Период передачи показаний закончился. " +
                            "Было доступно ${transitDays.getRangeDescription()}"
                }

                println("❌ [Repository] $message")
                return Result.failure(Exception(message))
            }

            val daysLeft = transitDays.daysUntilEnd(today)
            println("✅ [Repository] Период передачи активен (${transitDays.getRangeDescription()})")
            daysLeft?.let { println("   ⏰ Осталось дней: $it") }

            // ШАГ 2: Отправляем показания
            println("📤 [Repository] Отправляем показания:")
            println("   📊 Счётчик: ${counter.servName.trim()} (${counter.number.trim()})")
            println("   📈 Значение: $value")
            valueNight?.let { println("   🌙 Ночной тариф: $it") }

            // Текущее время в формате ISO 8601 с Z
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateString = dateFormat.format(Date())

            // Формируем значение для отправки
            val valueSending = if (counter.idTtype == "2T" && valueNight != null) {
                "$value;$valueNight"  // Для двухтарифного: "день;ночь"
            } else {
                value  // Для однотарифного: просто значение
            }

            val counterForInsert = CounterForInsertDto(
                idCnt = counter.idCnt,
                server = counter.server,
                dbName = counter.dbName,
                idA = counter.idA,
                `val` = valueSending,
                idType = counter.idType,
                date = dateString,
                datB = counter.datB
            )

            val requestBody = InsertCtrRequest(
                servDb = location,
                ctrForInsert = listOf(counterForInsert),
                notes = notes,
                category = 0
            )

            val response: HttpResponse = httpClient.post(
                urlString = "https://send.kvc-nn.ru/api/ControlIndications/InsertCtr"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // Проверяем статус ответа
            if (response.status.value in 200..299) {
                println("✅ [Repository] Показания успешно отправлены")
                Result.success(Unit)
            } else {
                println("❌ [Repository] Ошибка отправки: HTTP ${response.status.value}")
                Result.failure(Exception("Сервер вернул ошибку: HTTP ${response.status.value}"))
            }

        } catch (e: Exception) {
            println("❌ [Repository] Ошибка отправки показаний: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

}
