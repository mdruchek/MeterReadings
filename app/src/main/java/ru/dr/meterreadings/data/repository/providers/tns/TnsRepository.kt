package ru.dr.meterreadings.data.repository.providers.tns

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ru.dr.meterreadings.data.remote.dto.tns.TnsAccountDto
import ru.dr.meterreadings.data.remote.dto.tns.TnsAccountsResponseDto
import ru.dr.meterreadings.data.remote.dto.tns.TnsAppVersionResponse
import ru.dr.meterreadings.data.remote.dto.tns.TnsAuthData
import ru.dr.meterreadings.data.remote.dto.tns.TnsCounterDto
import ru.dr.meterreadings.data.remote.dto.tns.TnsCountersResponse
import ru.dr.meterreadings.data.remote.dto.tns.TnsUserAuthResponse
import ru.dr.meterreadings.data.remote.dto.tns.TnsUserAuthRequest
import ru.dr.meterreadings.data.remote.dto.tns.TnsRegionResponseDto
import ru.dr.meterreadings.data.remote.dto.tns.TnsRegionsResponseDto
import ru.dr.meterreadings.utils.safeNetworkCall
import ru.dr.meterreadings.utils.safeNetworkCallWithStatusHandlers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с API провайдера ТНС
 *
 * Базовый URL: https://мобил.tnse.ru
 */
@Singleton
class TnsRepository @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://mobile-api-rostov.tns-e.ru"
        private const val API_HASH = "b4c9554247f14b9a281f5f60df923f5e"
        //private const val API_USERNAME = "mobile-api-rostov"
        //private const val API_PASSWORD = "mobile-api-rostov"
        private const val APP_VERSION = "3.0.12"
        private const val BASIC_AUTH = "Basic bW9iaWxlLWFwaS1yb3N0b3Y6bW9iaWxlLWFwaS1yb3N0b3Y="
    }

//    private fun getBasicAuthHeader(): String {
//        val credentials = "$API_USERNAME:$API_PASSWORD"
//        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
//    }

    /**
     * Авторизация приложения на API
     * GET /api/v1/app/version?version=3.0.12
     */
    suspend fun authorizeApp(): Result<Boolean> {
        return try {
            println("🔐 [TnsRepository] Авторизация приложения...")
            val response = httpClient.get(
                urlString = "$BASE_URL/api/v1/app/version?version=$APP_VERSION"
            ) {
                header("user-agent", "Dart/3.9 (dart:io)")
                header("accept-encoding", "gzip")
                header("x-api-hash", API_HASH)
                header("authorization", BASIC_AUTH)
                header("x-device-id", "TE1A.240213.009")
                header("content-type", "application/json")
            }

            val versionResponse = response.body<TnsAppVersionResponse>()
            val isAuthorized = versionResponse.data.status == 1

            println("✅ [TnsRepository] Приложение авторизовано: $isAuthorized")
            Result.success(isAuthorized)
        } catch (e: Exception) {
            println("❌ [TnsRepository] Ошибка авторизации: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить список регионов ТНС
     *
     * API: GET /api/v1/contacts/regions
     *
     * @return Result со списком регионов
     */
    suspend fun getRegions(): Result<List<TnsRegionResponseDto>> {
        println("🔍 [TnsRepository] Загружаем регионы ТНС...")

        return safeNetworkCall {
            val response = httpClient.get(
                urlString = "$BASE_URL/api/v1/contacts/regions"
            ) {
                header("x-api-hash", API_HASH)
                header("accept", "application/json")
                header("accept-encoding", "gzip")
                header("content-type", "application/x-www-form-urlencoded")
                header("user-agent", "okhttp/4.12.0")
            }
            println("✅ [KvcRepository] HTTP ${response.status.value}")

            val responseBody = response.body<TnsRegionsResponseDto>()

            if (!responseBody.result) {
                throw Exception("API вернул ошибку: statusCode=${responseBody.statusCode}")
            }

            val regions = responseBody.data
            println("✅ [TnsRepository] Получено регионов: ${regions.size}")

            regions
        }
    }

    /**
     * Авторизация пользователя на API ТНС
     *
     * POST /api/v1/user/auth
     *
     * @param login Email или логин пользователя
     * @param password Пароль
     * @param regionCode Код региона (например, "nn")
     * @return Result с токенами доступа
     */
    suspend fun authorizeUser(
        login: String,
        password: String,
        regionCode: String
    ): Result<TnsAuthData> {
        return try {
            println("🔐 [TnsRepository] Авторизация пользователя: $login в регионе $regionCode")

            val requestBody = TnsUserAuthRequest(
                login = login,
                authType = "email",
                password = password,
                region = regionCode,
                platform = "android"
            )

            println("📤 [TnsRepository] Request Body: ${Json.encodeToString(requestBody)}")

            // ✅ Используем правильный URL с регионом
            val regionUrl = "https://mobile-api-$regionCode.tns-e.ru"

            val response = httpClient.post(
                urlString = "$regionUrl/api/v1/user/auth"
            ) {
                header("user-agent", "Dart/3.9 (dart:io)")
                header("x-api-hash", API_HASH)
                header("authorization", BASIC_AUTH)
                header("x-device-id", "TE1A.240213.009")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val authResponse = response.body<TnsUserAuthResponse>()

            // ✅ ПРОВЕРКА НА ОШИБКУ - если data == null
            if (authResponse.data == null) {
                val errorMsg = authResponse.errors?.firstOrNull()?.message
                    ?: authResponse.message
                    ?: "Неверный email или пароль"

                println("❌ [TnsRepository] Ошибка авторизации: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            // ✅ Дополнительная проверка успешности
            if (authResponse.result == false || authResponse.statusCode != 200) {
                val errorMsg = authResponse.message ?: "Неверный логин или пароль"
                println("❌ [TnsRepository] Авторизация отклонена: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            println("✅ [TnsRepository] Пользователь авторизован")
            println("   Access Token: ${authResponse.data.accessToken.take(20)}...")
            println("   Expires: ${authResponse.data.accessTokenExpires}")

            Result.success(authResponse.data)

        } catch (e: Exception) {
            println("❌ [TnsRepository] Ошибка авторизации: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Получить список лицевых счетов пользователя
     *
     * API: GET /api/v1/accounts
     *
     * Требует авторизации через Bearer token.
     *
     * @param accessToken JWT токен доступа
     * @param regionCode Код региона (например, "nn")
     * @return Result со списком аккаунтов
     *
     * ✅ Обработка HTTP ошибок через safeNetworkCall
     * ✅ БЕЗ проверки токенов (это делает Connector)
     *
     */
    suspend fun getAccounts(
        accessToken: String,
        regionCode: String
    ): Result<List<TnsAccountDto>> {
        println("🔍 [TnsRepository] Загружаем лицевые счета...")

        return safeNetworkCallWithStatusHandlers<List<TnsAccountDto>>(
            statusHandlers = mapOf(
                HttpStatusCode.OK to { response ->
                    val responseStatus = response.status.value
                    println("[TnsRepository] статус response ответа accounts: $responseStatus")
                    val responseBodyDto = response.body<TnsAccountsResponseDto>()
                    require(responseBodyDto.result) { "API error: ${responseBodyDto.statusCode}" }
                    val accounts: List<TnsAccountDto> = responseBodyDto.data
                    println("[TnsRepository] получено аккаунтов: ${accounts.size}")
                    println("[TnsRepository] получены аккаунты: $accounts")
                    accounts
                }
            )
        ) {
            httpClient.get("https://mobile-api-$regionCode.tns-e.ru/api/v1/accounts") {
                header("user-agent", "Dart/3.9 (dart:io)")
                header("x-api-hash", API_HASH)
                header("authorizationtest", "Bearer $accessToken")
                header("authorization", BASIC_AUTH)
            }
        }
    }

    /**
     * Получить список счётчиков для указанного лицевого счёта
     *
     * API: GET /api/v1/counters?account=521041038358
     *
     * Требует авторизации через Bearer token.
     *
     * @param accountNumber Номер лицевого счёта
     * @param accessToken JWT токен доступа
     * @param regionCode Код региона (например, "nn")
     * @return Result со списком счётчиков
     */
    suspend fun getCounters(
        accountNumber: String,
        accessToken: String,
        regionCode: String
    ): Result<List<TnsCounterDto>> {
        return safeNetworkCall {
            println("🔍 [TnsRepository] Загружаем счётчики для аккаунта $accountNumber...")

            // ✅ Используем URL с регионом
            val regionUrl = "https://mobile-api-$regionCode.tns-e.ru"

            val response = httpClient.get(
                urlString = "$regionUrl/api/v1/counters?account=$accountNumber"
            ) {
                header("user-agent", "Dart/3.9 (dart:io)")
                header("accept-encoding", "gzip")
                header("x-api-hash", API_HASH)
                header("authorizationtest", "Bearer $accessToken")
                header("authorization", BASIC_AUTH)
                header("x-device-id", "TE1A.240213.009")
                header("content-type", "application/json")
            }

            val countersResponse = response.body<TnsCountersResponse>()

            println("✅ [TnsRepository] Загружено счётчиков: ${countersResponse.data.size}")
            countersResponse.data.forEach { counter ->
                println("   ⚡ ID ${counter.counterId} | Тариф: ${counter.tariff} | Последнее: ${counter.lastReadings.firstOrNull()?.value}")
            }

            countersResponse.data
        }
    }

}
