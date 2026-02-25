package ru.dr.meterreadings.data.repository.providers.kvc

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaErrorDto
import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException
import ru.dr.meterreadings.data.remote.dto.kvc.KvcAccountRequestDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcAccountResponseDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcMeterHistoryResponseDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcMetersResponseDTO
import ru.dr.meterreadings.data.remote.dto.kvc.KvcRegionResponseDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcValidationErrorDto
import ru.dr.meterreadings.domain.exceptions.AccountNotFoundException
import ru.dr.meterreadings.domain.service.CaptchaService
import ru.dr.meterreadings.ui.components.CaptchaSession
import ru.dr.meterreadings.utils.safeNetworkCall
import ru.dr.meterreadings.utils.safeNetworkCallWithStatusHandlers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KvcRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val captchaService: CaptchaService
) {
    companion object {
        private const val BASE_URL = "https://send.kvc-nn.ru/api/ControlIndications"
    }

    // ==================== РЕГИОНЫ (БЕЗ АВТОРИЗАЦИИ) ====================

    suspend fun getRegions(): Result<List<KvcRegionResponseDto>> {
        println("🔍 [KvcRepository] Загружаем регионы КВС...")

        return safeNetworkCall {
            val response = httpClient.get("$BASE_URL/GetActiveCtrRegions")
            println("✅ [KvcRepository] HTTP ${response.status.value}")

            val regions = response.body<List<KvcRegionResponseDto>>()
            println("✅ [KvcRepository] Получено регионов: ${regions.size}")

            regions
        }
    }

    // ==================== АККАУНТ (ТРЕБУЕТ КАПЧУ) ====================

    /**
     * ⚠️ НЕ ВЫЗЫВАЙ НАПРЯМУЮ!
     * Используй только через safeKvcCall() в KvcConnector
     */
    // Repository — возвращает DTO!
    suspend fun getAccount(
        accountNumber: String,
        regionId: Int,
        session: CaptchaSession
    ): Result<KvcAccountResponseDto> {
        println("🔍 [KvcRepository] Загружаем лицевые счета...")

        return safeNetworkCallWithStatusHandlers<KvcAccountResponseDto>(
            statusHandlers = mapOf(
                HttpStatusCode.OK to { response ->
                    val responseStatus = response.status.value
                    println("🔍 [KvcRepository] статус response загрузки account: $responseStatus")
                    val account: KvcAccountResponseDto = response.body()
                    println("🔍 [KvcRepository] получен аккаунт: $account")
                    account
                },
                HttpStatusCode.Unauthorized to { response ->
                    // ✅ Кастомная обработка 401
                    val error = response.body<CaptchaErrorDto>()
                    throw CaptchaRequiredException(error.error ?: "Капча невалидна")
                }
            )
        ) {
            httpClient.post("$BASE_URL/GetAbonentInfo") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)

                headers {
                    append(HttpHeaders.Origin, "https://send.kvc-nn.ru")
                    append(HttpHeaders.Referrer, "https://send.kvc-nn.ru/")
                    append(HttpHeaders.UserAgent, session.userAgent)
                }

                setBody(
                    KvcAccountRequestDto(
                        account = accountNumber,
                        region = regionId,
                        captchaToken = session.token,
                        password = null
                    )
                )
            }
        }
    }

    // ==================== СЧЁТЧИКИ (не ТРЕБУЕТ КАПЧУ) ====================
    /**
     * ⚠️ Вызывается после getAccount, поэтому уже с валидной капчей
     */
    suspend fun getMeters(
        abonentId: String
    ): Result<List<KvcMetersResponseDTO>> {
        println("🔍 [KvcRepository] getMeters для abonentId=$abonentId")

        return safeNetworkCallWithStatusHandlers<List<KvcMetersResponseDTO>>(
            statusHandlers = mapOf(
                HttpStatusCode.OK to { response ->
                    val meters: List<KvcMetersResponseDTO> = response.body()
                    println("✅ [KvcRepository] Получено счётчиков: ${meters.size}")
                    meters.forEach { meter ->
                        println("   - ${meter.type}: ${meter.number} (${meter.lastFirstValue})")
                    }
                    meters
                },
                HttpStatusCode.BadRequest to { response ->
                    val errors = try {
                        response.body<List<KvcValidationErrorDto>>()
                    } catch (e: Exception) {
                        println("⚠️ [KvcRepository] Не удалось распарсить ошибку: ${e.message}")
                        listOf(KvcValidationErrorDto(
                            fieldName = "AbonentId",
                            errors = listOf("Абонент не найден")
                        ))
                    }

                    val errorMessage = errors.firstOrNull()?.errors?.firstOrNull()
                        ?: "Абонент не найден"

                    println("❌ [KvcRepository] Ошибка 400: $errorMessage")
                    println("   abonentId: $abonentId")

                    throw AccountNotFoundException(errorMessage)
                }
            )
        ) {
            httpClient.get("$BASE_URL/GetCntList") {
                parameter("abonentId", abonentId)

                headers {
                    append(HttpHeaders.Accept, "*/*")  // ✅ Как в HAR
                    append(HttpHeaders.Referrer, "https://send.kvc-nn.ru/")
                }
            }
        }
    }

    // ==================== ИСТОРИЯ (БЕЗ КАПЧИ) ====================
    suspend fun getMeterHistory(
        deviceId: String // UUID из GetCounters
    ): Result<List<KvcMeterHistoryResponseDto>> {
        println("KvcRepository: getMeterHistory для deviceId=$deviceId")

        return safeNetworkCallWithStatusHandlers<List<KvcMeterHistoryResponseDto>>(
            statusHandlers = mapOf(
                HttpStatusCode.OK to { response ->
                    val meterHistory: List<KvcMeterHistoryResponseDto> = response.body()
                    println("✅ [KvcRepository] Получена история: ${meterHistory.size}")
                    meterHistory.forEach { periodHistory ->
                        println("   - ${periodHistory.submissionPeriod}: ${periodHistory.lastValue} (${periodHistory.prevValue})")
                    }
                    meterHistory
                },
                HttpStatusCode.BadRequest to { response ->
                    val errors = try {
                        response.body<List<KvcValidationErrorDto>>()
                    } catch (e: Exception) {
                        println("⚠️ [KvcRepository] Не удалось распарсить ошибку: ${e.message}")
                        listOf(KvcValidationErrorDto(
                            fieldName = "DeviceId",
                            errors = listOf("Прибор не найден")
                        ))
                    }

                    val errorMessage = errors.firstOrNull()?.errors?.firstOrNull()
                        ?: "Прибор не найден"

                    println("❌ [KvcRepository] Ошибка 400: $errorMessage")
                    println("   DeviceId: $deviceId")

                    throw AccountNotFoundException(errorMessage)
                }
            )
        ) {
            httpClient.get("$BASE_URL/GetCtrList") {
                parameter("deviceId", deviceId)

                headers {
                    append(HttpHeaders.Accept, "*/*")  // ✅ Как в HAR
                    append(HttpHeaders.Referrer, "https://send.kvc-nn.ru/")
                }
            }
        }
    }

//    // ==================== ОТПРАВКА ПОКАЗАНИЙ (БЕЗ КАПЧИ) ====================
//
//    suspend fun submitReading(
//        counter: KvcMetersDto,
//        location: KvcLocationDto,
//        value: String,
//        valueNight: String?
//    ): Result<Unit> = withContext(Dispatchers.IO) {
//        println("KvcRepository: submitReading для счётчика ${counter.idCnt}")
//
//        safeNetworkCall {
//            val response: HttpResponse = httpClient.post("$BASE_URL/InsertCtr") {
////                contentType(ContentType.Application.Json)
////                setBody(SubmitReadingRequest.fromCounter(
////                    counter = counter,
////                    location = location,
////                    value = value,
////                    valueNight = valueNight
////                ))
//            }
//
//            val body = response.bodyAsText()
//            println("KvcRepository: submitReading response: $body")
//
//            if (response.status != HttpStatusCode.OK) {
//                throw Exception("HTTP ${response.status}")
//            }
//        }
//    }
}
