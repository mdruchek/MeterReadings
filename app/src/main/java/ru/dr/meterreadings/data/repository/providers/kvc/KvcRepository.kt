package ru.dr.meterreadings.data.repository.providers.kvc

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaErrorDto
import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException
import ru.dr.meterreadings.data.remote.dto.kvc.GetAbonentInfoRequest
import ru.dr.meterreadings.data.remote.dto.kvc.KvcAccountInfoDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcLocationDto
import ru.dr.meterreadings.data.remote.dto.kvc.KvcRegionDto
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.domain.service.CaptchaService
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.ui.components.CaptchaSession
import ru.dr.meterreadings.utils.safeNetworkCall
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

    suspend fun getRegions(): Result<List<KvcRegionDto>> = withContext(Dispatchers.IO) {
        println("KvcRepository: Получение регионов...")

        safeNetworkCall {
            val response: HttpResponse = httpClient.get("$BASE_URL/GetActiveCtrRegions")
            json.decodeFromString(response.bodyAsText())
        }
    }

    // ==================== LOCATIONS (БЕЗ АВТОРИЗАЦИИ) ====================

    suspend fun getLocationsForRegion(regionId: Int): Result<List<KvcLocationDto>> =
        withContext(Dispatchers.IO) {
            println("KvcRepository: Получение locations для региона $regionId...")

            safeNetworkCall {
                val response: HttpResponse = httpClient.get("$BASE_URL/GetLocationsForRegion") {
                    parameter("idRegion", regionId)
                }
                json.decodeFromString(response.bodyAsText())
            }
        }

    // ==================== АККАУНТ (ТРЕБУЕТ КАПЧУ) ====================

    /**
     * ⚠️ НЕ ВЫЗЫВАЙ НАПРЯМУЮ!
     * Используй только через safeKvcCall() в KvcConnector
     */
    suspend fun getAccount(
        accountNumber: String,
        regionId: Int,
        session: CaptchaSession
    ): GetAccounts.AccountInfo = withContext(Dispatchers.IO) {
        println("[KvcRepository] getAccount account=$accountNumber, region=$regionId")

        println("🔐 [KvcRepository] CaptchaSession:")
        println("   Token (first 80): ${session.token.take(80)}")
        println("   Cookies: ${session.cookies}")
        println("   User-Agent: ${session.userAgent}")

        val requestBody = GetAbonentInfoRequest(
            account = accountNumber,
            region = regionId,
            captchaToken = session.token,
            password = null
        )

        println("📤 [KvcRepository] Request body:")
        println("   account: ${requestBody.account}")
        println("   region: ${requestBody.region}")
        println("   captchaToken (first 80): ${requestBody.captchaToken.take(80)}")

        val response = httpClient.post("https://send.kvc-nn.ru/api/ControlIndications/GetAbonentInfo") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)

            headers {
                append(HttpHeaders.Origin, "https://send.kvc-nn.ru")  // ✅ Hardcode URL
                append(HttpHeaders.Referrer, "https://send.kvc-nn.ru/")  // ✅ Hardcode URL с /
                append(HttpHeaders.UserAgent, session.userAgent)  // ✅ Используем session
            }

            setBody(requestBody)
        }

        when (response.status.value) {
            200 -> {
                val body = response.body<KvcAccountInfoDto>()
                println("✅ [KvcRepository] Account found: ${body.id}")
                GetAccounts.AccountInfo(
                    accountNumber = body.account,
                    address = body.address,
                    login = null,
                    submissionStartDay = body.first,
                    submissionEndDay = body.last,
                    additionalInfo = null
                )
            }
            401 -> {
                val error = response.body<CaptchaErrorDto>()
                println("❌ [KvcRepository] 401: ${error.error}")
                println("❌ [KvcRepository] Request headers:")
                println("   - Origin: https://send.kvc-nn.ru")
                println("   - Referer: https://send.kvc-nn.ru/")
                println("   - User-Agent: ${session.userAgent}")
                println("   - captchaToken (first 80): ${session.token.take(80)}")
                throw CaptchaRequiredException(error.error ?: "Капча не пройдена")
            }
            else -> {
                println("❌ [KvcRepository] Unexpected status: ${response.status}")
                throw Exception("HTTP ${response.status.value}")
            }
        }
    }





    // ==================== СЧЁТЧИКИ (ТРЕБУЕТ КАПЧУ) ====================

//    /**
//     * ⚠️ Вызывается после getAccount, поэтому уже с валидной капчей
//     */
//    suspend fun getMeters(abonentId: String): Result<List<KvcMetersDto>> =
//        withContext(Dispatchers.IO) {
//            println("KvcRepository: getMeters для abonentId=$abonentId")
//
//            safeNetworkCall {
//                val response: HttpResponse = httpClient.get("$BASE_URL/GetCntList") {
//                    parameter("abonentId", abonentId)
//                }
//                json.decodeFromString(response.bodyAsText())
//            }
//        }
//
//    // ==================== ИСТОРИЯ (БЕЗ КАПЧИ) ====================
//    suspend fun getMeterHistory(
//        deviceId: String // UUID из GetCounters
//    ): Result<List<KvcMeterHistoryDto>> = withContext(Dispatchers.IO) {
//        println("KvcRepository: getMeterHistory для deviceId=$deviceId")
//
//        safeNetworkCall<List<KvcMeterHistoryDto>> {
//            val response: HttpResponse = httpClient.get("$BASE_URL/GetCtrList") { // ✅ GET
//                parameter("deviceId", deviceId) // ✅ Query parameter
//            }
//            json.decodeFromString(response.bodyAsText())
//        }
//    }
//
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
