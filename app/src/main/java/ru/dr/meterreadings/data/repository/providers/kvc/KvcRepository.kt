package ru.dr.meterreadings.data.repository.providers.kvc

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.dr.meterreadings.data.remote.dto.kvc.*
import ru.dr.meterreadings.utils.safeNetworkCall
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KvcRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) {
    companion object {
        private const val BASE_URL = "https://send.kvc-nn.ru/api/ControlIndications"
    }

    suspend fun getRegions(): Result<List<KvcRegionDto>> = safeNetworkCall {
        println("KvcRepository: Получение регионов...")
        val response = httpClient.get {
            url("$BASE_URL/GetActiveCtrRegions")
        }
        val regions = response.body<List<KvcRegionDto>>()
        println("KvcRepository: Получено регионов: ${regions.size}")
        regions
    }

    suspend fun getLocationsForRegion(regionId: Int): Result<List<KvcLocationDto>> = try {
        println("KvcRepository: Получение н/п для regionId=$regionId...")
        val response = httpClient.post {
            url("$BASE_URL/GetLocationsForRegion")
            parameter("idRegion", regionId)
        }
        val locations = response.body<List<KvcLocationDto>>()
        println("KvcRepository: Получено н/п: ${locations.size}")
        Result.success(locations)
    } catch (e: Exception) {
        println("KvcRepository: Ошибка getLocationsForRegion: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    /**
     * ✅ ИСПРАВЛЕНО: точное соответствие HAR
     */
    /**
     * ✅ ИСПРАВЛЕНО: точное соответствие HAR
     * POST /GetAbonentInfo
     * Body: { "account": "...", "region": 15, "captchaToken": "...", "password": null }
     */
    // KvcRepository.kt
    suspend fun getAccount(
        accountNumber: String,
        regionId: Int,
        captchaToken: String? = null
    ): Result<KvcAccountInfoDto> = withContext(Dispatchers.IO) {
        try {
            println("KvcRepository: getAccount account=$accountNumber, region=$regionId")

            val response: HttpResponse = httpClient.post("$BASE_URL/GetAbonentInfo") {
                contentType(ContentType.Application.Json)

                // ✅ ТОКЕН ТОЛЬКО В ТЕЛЕ!
                setBody(GetAbonentInfoRequest(
                    account = accountNumber,
                    region = regionId,
                    captchaToken = captchaToken ?: "",  // Пустая строка если нет токена
                    password = null
                ))

                // ❌ НЕТ ЗАГОЛОВКОВ!
            }

            val body = response.bodyAsText()
            println("KvcRepository: Response status=${response.status}")
            println("KvcRepository: Response body: ${body.substring(0, minOf(200, body.length))}")

            if (response.status == HttpStatusCode.OK) {
                val account = json.decodeFromString<KvcAccountInfoDto>(body)
                println("✅ KvcRepository: Аккаунт успешно получен")
                Result.success(account)
            } else {
                println("❌ KvcRepository: HTTP ${response.status}")
                Result.failure(Exception("HTTP ${response.status}"))
            }

        } catch (e: Exception) {
            println("❌ KvcRepository: Ошибка: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }


    /**
     * ✅ ИСПРАВЛЕНО: GET запрос
     */
    suspend fun getMeters(
        abonentId: String
    ): Result<List<KvcMetersDto>> = try {
        println("KvcRepository: getMeters abonentId=$abonentId")

        val response = httpClient.get {
            url("$BASE_URL/GetCntList")
            parameter("abonentId", abonentId)
        }

        val counters = response.body<List<KvcMetersDto>>()
        println("KvcRepository: ✅ Получено счётчиков: ${counters.size}")

        Result.success(counters)
    } catch (e: Exception) {
        println("KvcRepository: ❌ getMeters error: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    suspend fun getTransmissionPeriod(
        location: KvcLocationDto,
        accountNumber: String
    ): Result<KvcTransmissionPeriodDto> = try {
        println("KvcRepository: getTransmissionPeriod")

        val requestBody = GetTransmissionPeriodRequestDto(
            servDb = location,
            lc = accountNumber
        )

        val response = httpClient.post {
            url("$BASE_URL/GetCtrDays")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val transitDays = response.body<KvcTransmissionPeriodDto>()
        println("KvcRepository: Период: ${transitDays.first}-${transitDays.last}")

        Result.success(transitDays)
    } catch (e: Exception) {
        println("KvcRepository: ❌ getTransmissionPeriod error: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    suspend fun getMeterHistory(
        location: KvcLocationDto,
        accountNumber: String,
        meterId: Int
    ): Result<List<KvcMeterHistoryDto>> = try {
        println("KvcRepository: getMeterHistory idCnt=$meterId")

        val requestBody = GetMetersRequestDto(
            servDb = location,
            lc = accountNumber,
            idCnt = meterId
        )

        val response = httpClient.post {
            url("$BASE_URL/GetCtrList")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val history = response.body<List<KvcMeterHistoryDto>>()
        println("KvcRepository: История: ${history.size} записей")

        Result.success(history)
    } catch (e: Exception) {
        println("KvcRepository: ❌ getMeterHistory error: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }

    /**
     * Отправить показания счётчика
     * POST /InsertCtr
     */
    suspend fun submitReading(
        counter: KvcMetersDto,
        location: KvcLocationDto,
        value: String,
        valueNight: String? = null,
        notes: String = ""
    ): Result<Unit> {
        return try {
            // 1. Проверка периода передачи
            println("KvcRepository: Проверка периода...")
            val transitDaysResult = getTransmissionPeriod(location, counter.lc.trim())
            if (transitDaysResult.isFailure) {
                return Result.failure(transitDaysResult.exceptionOrNull()!!)
            }

            val transitDays = transitDaysResult.getOrThrow()
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            if (!transitDays.canSubmitToday(today)) {
                val message = if (today < transitDays.first) {
                    "Передача показаний доступна с ${transitDays.first} числа"
                } else {
                    "Период передачи показаний закончился (${transitDays.getRangeDescription()})"
                }
                println("KvcRepository: ❌ $message")
                return Result.failure(Exception(message))
            }

            println("KvcRepository: ✅ Период подходит")

            // 2. Отправка показаний
            println("KvcRepository: Отправка показаний...")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val dateString = dateFormat.format(Date())

            val valueSending = if (counter.idTtype == "2T" && valueNight != null) {
                "$value/$valueNight"
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

            val response: HttpResponse = httpClient.post {
                url("$BASE_URL/InsertCtr")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.value in 200..299) {
                println("KvcRepository: ✅ Показания успешно отправлены")
                Result.success(Unit)
            } else {
                println("KvcRepository: ❌ HTTP ${response.status.value}")
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            println("KvcRepository: ❌ submitReading error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
