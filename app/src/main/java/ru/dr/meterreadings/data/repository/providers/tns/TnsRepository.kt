package ru.dr.meterreadings.data.repository.providers.tns

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ru.dr.meterreadings.data.remote.dto.tns.TnsAppVersionResponse
import ru.dr.meterreadings.data.remote.dto.tns.TnsAuthData
import ru.dr.meterreadings.data.remote.dto.tns.TnsUserAuthResponse
import ru.dr.meterreadings.data.remote.dto.tns.TnsUserAuthRequest
import ru.dr.meterreadings.data.remote.dto.tns.TnsRegionDto
import ru.dr.meterreadings.data.remote.dto.tns.TnsRegionsResponse
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
    suspend fun getRegions(): Result<List<TnsRegionDto>> {
        return try {
            println("🔍 [TnsRepository] Загружаем регионы ТНС...")

            val response = httpClient.get(
                urlString = "$BASE_URL/api/v1/contacts/regions"
            ) {
                // ✅ ДОБАВЛЯЕМ ЗАГОЛОВКИ ИЗ ПЕРЕХВАЧЕННОГО ЗАПРОСА
                header("x-api-hash", API_HASH)
                header("accept", "application/json")
                header("accept-encoding", "gzip")
                header("content-type", "application/x-www-form-urlencoded")
                header("user-agent", "okhttp/4.12.0")
            }

            val regionsResponse = response.body<TnsRegionsResponse>()

            println("✅ [TnsRepository] Загружено регионов: ${regionsResponse.data.size}")
            regionsResponse.data.forEach { region ->
                println("   📍 ${region.name} (${region.code})")
            }

            Result.success(regionsResponse.data)
        } catch (e: Exception) {
            println("❌ [TnsRepository] Ошибка загрузки регионов: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
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
}