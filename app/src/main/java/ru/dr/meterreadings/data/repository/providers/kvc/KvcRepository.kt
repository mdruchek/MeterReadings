package ru.dr.meterreadings.data.repository.providers.kvc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.dr.meterreadings.data.remote.dto.kvc.CounterForInsertDto
import ru.dr.meterreadings.data.remote.dto.kvc.GetAbonentInfoRequest
import ru.dr.meterreadings.data.remote.dto.kvc.GetCntListRequest
import ru.dr.meterreadings.data.remote.dto.kvc.GetTransmissionPeriodRequestDto
import ru.dr.meterreadings.data.remote.dto.kvc.GetMetersRequestDto
import ru.dr.meterreadings.data.remote.dto.kvc.InsertCtrRequest
import ru.dr.meterreadings.data.remote.dto.kvc.KvcAccountInfoDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcMetersDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcMeterHistoryDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcLocationDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcRegionDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcTransmissionPeriodDto
import ru.dr.meterreadings.utils.safeNetworkCall
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с API провайдера КВЦ (Нижний Новгород)
 *
 * Содержит все методы для взаимодействия с send.kvc-nn.ru:
 * - Получение регионов и конфигураций БД
 * - Поиск абонента по лицевому счёту
 * - Получение списка счётчиков
 * - Проверка разрешённого периода передачи
 * - Отправка показаний счётчиков
 */
@Singleton
class KvcRepository @Inject constructor(
    private val httpClient: HttpClient
) {

    companion object {
        private const val BASE_URL = "https://send.kvc-nn.ru/api/ControlIndications"
    }

    /**
     * Получить список регионов КВЦ
     *
     * Возвращает список районов Нижегородской области где работает КВЦ.
     *
     * @return Result со списком регионов (57 районов)
     *
     * API: POST /GetActiveCtrRegions
     */
    suspend fun getRegions(): Result<List<KvcRegionDto>> {
        return safeNetworkCall {
            println("🔍 [KvcRepository] Загружаем регионы КВЦ...")

            val response = httpClient.get(
                urlString = "$BASE_URL/GetActiveCtrRegions"
            )

            val regions = response.body<List<KvcRegionDto>>()

            println("✅ [KvcRepository] Загружено регионов: ${regions.size}")

            regions
        }
    }

    /**
     * Получить список конфигураций БД для региона КВЦ
     *
     * Возвращает список баз данных провайдеров услуг в выбранном регионе.
     * Каждая БД обычно соответствует одной организации (Водоканал, Газ и т.д.)
     *
     * @param regionId ID региона (из getRegions)
     * @return Result со списком конфигураций БД
     *
     * API: POST /GetLocationsForRegion?idRegion={regionId}
     */
    suspend fun getLocationsForRegion(regionId: Int): Result<List<KvcLocationDto>> {
        return try {
            println("🔍 [KvcRepository] Загружаем конфигурации БД для региона $regionId...")

            val response = httpClient.post(
                urlString = "$BASE_URL/GetLocationsForRegion"
            ) {
                parameter("idRegion", regionId)
            }

            val locations = response.body<List<KvcLocationDto>>()

            println("✅ [KvcRepository] Загружено конфигураций БД: ${locations.size}")
            Result.success(locations)
        } catch (e: Exception) {
            println("❌ [KvcRepository] Ошибка загрузки конфигураций: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить информацию об абоненте КВЦ по лицевому счёту
     *
     * Ищет абонента во всех БД региона по номеру лицевого счёта.
     *
     * @param locations Список конфигураций БД (из getLocationsForRegion)
     * @param accountNumber Номер лицевого счёта
     * @param target Цель поиска (0 = поиск по всем БД)
     * @return Result с информацией об абоненте
     *
     * API: POST /GetAbonentInfo
     * Body: { servDbs: [...], lc: "...", target: 0 }
     */
    suspend fun getAccount(
        locations: List<KvcLocationDto>,
        accountNumber: String,
        target: Int = 0
    ): Result<KvcAccountInfoDto> {
        return try {
            println("🔍 [KvcRepository] Получаем данные абонента: ЛС=$accountNumber")

            val requestBody = GetAbonentInfoRequest(
                servDbs = locations,
                lc = accountNumber,
                target = target
            )

            val response = httpClient.post(
                urlString = "$BASE_URL/GetAbonentInfo"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val accountInfo = response.body<KvcAccountInfoDto>()

            println("✅ [KvcRepository] Получены данные абонента")
            println("   🏠 Адрес: ${accountInfo.getFullAddress()}")

            Result.success(accountInfo)
        } catch (e: Exception) {
            println("❌ [KvcRepository] Ошибка получения данных абонента: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить список счётчиков абонента КВЦ
     *
     * Возвращает все счётчики (вода, газ, электричество) для конкретного лицевого счёта.
     *
     * @param location Конфигурация БД провайдера (из getAbonentInfo → location)
     * @param accountNumber Номер лицевого счёта
     * @return Result со списком счётчиков
     *
     * API: POST /GetCntList
     * Body: { servDb: {...}, lc: "..." }
     */
    suspend fun getMeters(
        location: KvcLocationDto,
        accountNumber: String
    ): Result<List<KvcMetersDto>> {
        return try {
            println("🔍 [KvcRepository] Получаем счётчики: ЛС=$accountNumber")

            val requestBody = GetCntListRequest(
                servDb = location,
                lc = accountNumber
            )

            val response = httpClient.post(
                urlString = "$BASE_URL/GetCntList"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val counters = response.body<List<KvcMetersDto>>()

            println("✅ [KvcRepository] Получено счётчиков: ${counters.size}")
            Result.success(counters)
        } catch (e: Exception) {
            println("❌ [KvcRepository] Ошибка получения счётчиков: ${e.message}")
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
     * @param location Конфигурация БД провайдера
     * @param accountNumber Номер лицевого счёта
     * @return Result с диапазоном разрешённых дней (first..last)
     *
     * API: POST /GetCtrDays
     * Body: { servDb: {...}, lc: "..." }
     * Response: { first: 15, last: 22 }
     */
    suspend fun getTransmissionPeriod(
        location: KvcLocationDto,
        accountNumber: String
    ): Result<KvcTransmissionPeriodDto> {
        return try {
            println("🔍 [KvcRepository] Получаем разрешённые дни передачи")

            val requestBody = GetTransmissionPeriodRequestDto(
                servDb = location,
                lc = accountNumber
            )

            val response = httpClient.post(
                urlString = "$BASE_URL/GetCtrDays"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val transitDays = response.body<KvcTransmissionPeriodDto>()

            println("✅ [KvcRepository] Диапазон: ${transitDays.first}-${transitDays.last}")
            Result.success(transitDays)
        } catch (e: Exception) {
            println("❌ [KvcRepository] Ошибка получения разрешённых дней: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить историю показаний счётчика КВЦ
     *
     * Возвращает список всех переданных показаний по счётчику за последние месяцы.
     * Полезно для:
     * - Проверки минимально допустимого показания
     * - Отображения расхода за прошлый месяц
     * - Истории передачи
     *
     * @param location Конфигурация БД провайдера
     * @param accountNumber Номер лицевого счёта
     * @param meterId ID счётчика (idCnt)
     * @return Result со списком истории (отсортирован от новых к старым)
     *
     * API: POST /GetCtrList
     * Body: { servDb: {...}, lc: "...", idCnt: 58946 }
     */
    suspend fun getMeterHistory(
        location: KvcLocationDto,
        accountNumber: String,
        meterId: Int
    ): Result<List<KvcMeterHistoryDto>> {
        return try {
            println("🔍 [KvcRepository] Получаем историю показаний: idCnt=$meterId")

            val requestBody = GetMetersRequestDto(
                servDb = location,
                lc = accountNumber,
                idCnt = meterId
            )

            val response = httpClient.post(
                urlString = "$BASE_URL/GetCtrList"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val history = response.body<List<KvcMeterHistoryDto>>()
            println("✅ [KvcRepository] Получено записей истории: ${history.size}")

            Result.success(history)
        } catch (e: Exception) {
            println("❌ [KvcRepository] Ошибка получения истории: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Отправить показания счётчика КВЦ
     *
     * Передаёт новые показания на сервер КВЦ.
     * ВАЖНО: Автоматически проверяет разрешённый период передачи перед отправкой.
     *
     * @param counter Счётчик (из getCounters)
     * @param location Конфигурация БД (из getAbonentInfo → location)
     * @param value Значение показания (для однотарифного или дневной тариф)
     * @param valueNight Значение ночного тарифа (для двухтарифного счётчика)
     * @param notes Комментарий к передаче (по умолчанию "Передано через приложение")
     * @return Result<Unit> - успех или ошибка
     *
     * API: POST /InsertCtr
     * Body: { servDb: {...}, ctrForInsert: [...], notes: "...", category: 0 }
     */
    suspend fun submitReading(
        counter: KvcMetersDto,
        location: KvcLocationDto,
        value: String,
        valueNight: String? = null,
        notes: String = "Передано через приложение"
    ): Result<Unit> {
        return try {
            // ШАГ 1: Проверяем разрешённый период
            println("🔍 [KvcRepository] Проверяем период передачи...")

            val transitDaysResult = getTransmissionPeriod(location, counter.lc.trim())
            if (transitDaysResult.isFailure) {
                return Result.failure(Exception("Не удалось проверить разрешённые дни"))
            }

            val transitDays = transitDaysResult.getOrThrow()
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            if (!transitDays.canSubmitToday(today)) {
                val message = if (today < transitDays.first) {
                    "Передача доступна ${transitDays.getRangeDescription()}"
                } else {
                    "Период передачи закончился"
                }
                println("❌ [KvcRepository] $message")
                return Result.failure(Exception(message))
            }

            println("✅ [KvcRepository] Период активен")

            // ШАГ 2: Формируем и отправляем запрос
            println("📤 [KvcRepository] Отправляем показания...")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val dateString = dateFormat.format(Date())

            // Для двухтарифных счётчиков объединяем значения через ";"
            val valueSending = if (counter.idTtype == "2T" && valueNight != null) {
                "$value;$valueNight"
            } else {
                value
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
                urlString = "$BASE_URL/InsertCtr"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // Проверяем статус ответа
            if (response.status.value in 200..299) {
                println("✅ [KvcRepository] Показания успешно отправлены")
                Result.success(Unit)
            } else {
                println("❌ [KvcRepository] HTTP ${response.status.value}")
                Result.failure(Exception("Ошибка сервера: HTTP ${response.status.value}"))
            }

        } catch (e: Exception) {
            println("❌ [KvcRepository] Ошибка отправки показаний: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
