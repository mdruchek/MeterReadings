package ru.dr.meterreadings.utils

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import ru.dr.meterreadings.domain.connector.AuthException
import ru.dr.meterreadings.domain.service.AuthService
import ru.dr.meterreadings.domain.service.TokenResult
import java.net.UnknownHostException
import java.net.SocketTimeoutException

/**
 *
 * Обёртка для Repository-запросов с авторизацией
 *
 * Комбинирует проверку авторизации + обработку исключений.
 * Используй когда block возвращает Result<T> (например, Repository).
 *
 * @param authService Сервис авторизации
 * @param providerId ID провайдера
 * @param login Логин пользователя
 * @param regionId ID региона (опционально)
 * @param block Блок с Repository-запросом, возвращает Result<T>
 * @return Result с данными или ошибкой (AuthException + NetworkException)
 */
suspend fun <T> safeAuthenticatedCall(
    authService: AuthService,
    providerId: Long,
    login: String,
    regionId: String? = null,
    block: suspend (accessToken: String) -> Result<T>
): Result<T> {
    val tokenResult = authService.getValidAccessToken(providerId, login, regionId)

    return when (tokenResult) {
        is TokenResult.Success -> {
            // ✅ Оборачиваем в safeNetworkCall для обработки непойманных исключений
            safeNetworkCall {
                // ✅ Разворачиваем Result из Repository
                block(tokenResult.accessToken).getOrThrow()
            }
        }
        is TokenResult.Error -> {
            Result.failure(AuthException(tokenResult.authError))
        }
    }
}

/**
 * Обёртка для запросов с кастомной обработкой HTTP статусов
 *
 * @param statusHandlers Map<HttpStatusCode, suspend (HttpResponse) -> T>
 * @param block Блок с HTTP запросом
 */
/**
 * Обёртка для запросов с кастомной обработкой HTTP статусов
 */
suspend inline fun <reified T> safeNetworkCallWithStatusHandlers(
    statusHandlers: Map<HttpStatusCode, suspend (HttpResponse) -> T> = emptyMap(),
    crossinline block: suspend () -> HttpResponse
): Result<T> {
    return safeNetworkCall {
        try {
            val response = block()

            // Проверяем кастомный обработчик
            val handler = statusHandlers[response.status]
            if (handler != null) {
                handler(response)
            } else {
                // Стандартный парсинг
                response.body<T>()
            }

        } catch (e: ClientRequestException) {
            // Проверяем обработчик для ошибки
            val handler = statusHandlers[e.response.status]
            if (handler != null) {
                handler(e.response)
            } else {
                throw e  // Пробрасываем в safeNetworkCall
            }
        }
    }
}

/**
 * Универсальная обёртка для сетевых запросов с обработкой ошибок
 *
 * Автоматически ловит и преобразует типовые ошибки в понятные сообщения.
 * Техническая информация логируется в консоль для отладки.
 *
 * @param block Suspend-функция с сетевым запросом
 * @return Result с данными или человеческой ошибкой
 */
suspend fun <T> safeNetworkCall(
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())

    } catch (e: ClientRequestException) {
        // ✅ HTTP ошибки (4xx, 5xx)
        val statusCode = e.response.status.value
        val url = e.response.call.request.url

        println("❌ [Network] HTTP $statusCode: $url")
        println("   Request: ${e.response.call.request.method.value}")
        println("   Response: ${e.response.status}")

        // ✅ Пытаемся прочитать body для деталей
        var responseBody: String? = null
        try {
            responseBody = e.response.bodyAsText()
            if (responseBody.isNotEmpty()) {
                println("   Body (первые 500 символов):")
                println("   ${responseBody.take(500)}")
            }
        } catch (bodyError: Exception) {
            println("   Body: не удалось прочитать (${bodyError.javaClass.simpleName})")
        }

        e.printStackTrace()

        // ✅ Определяем человеческое сообщение
        val errorMessage = when {
            // Проверяем содержимое body на специфичные ошибки
            responseBody?.contains("Method Not Allowed", ignoreCase = true) == true ->
                "Метод не поддерживается сервером"

            responseBody?.contains("Not Found", ignoreCase = true) == true ->
                "Данные не найдены"

            // Стандартные HTTP коды
            statusCode == 400 -> "Некорректный запрос"
            statusCode == 401 -> "Требуется авторизация"
            statusCode == 403 -> "Доступ запрещён"
            statusCode == 404 -> "Данные не найдены"
            statusCode == 405 -> "Метод не поддерживается сервером"
            statusCode == 408 -> "Превышено время ожидания"
            statusCode == 500 -> "Внутренняя ошибка сервера"
            statusCode == 502 -> "Сервер временно недоступен"
            statusCode == 503 -> "Сервис недоступен"

            else -> "Ошибка сервера (HTTP $statusCode)"
        }

        Result.failure(Exception(errorMessage))

    } catch (e: SerializationException) {
        // ✅ Ошибки парсинга JSON
        println("❌ [Network] Ошибка парсинга JSON")
        println("   Класс: ${e.javaClass.simpleName}")
        println("   Сообщение: ${e.message}")
        println("   Причина: ${e.cause?.message}")
        e.printStackTrace()

        // ✅ ВАЖНО: Проверяем, не была ли это ошибка из-за неправильного типа ответа
        val message = when {
            e.message?.contains("Expected response body") == true ->
                "Сервер вернул некорректный ответ"

            e.message?.contains("ByteBufferChannel") == true ->
                "Сервер вернул данные в неправильном формате"

            e.message?.contains("Kotlin reflection is not available") == true ->
                "Ошибка обработки ответа сервера"

            else -> "Некорректный ответ от сервера"
        }

        Result.failure(Exception(message))

    } catch (e: UnknownHostException) {
        println("❌ [Network] Нет подключения к хосту")
        println("   Хост: ${e.message}")
        e.printStackTrace()

        Result.failure(Exception("Проверьте подключение к интернету"))

    } catch (e: SocketTimeoutException) {
        println("❌ [Network] Превышено время ожидания")
        println("   Сообщение: ${e.message}")
        e.printStackTrace()

        Result.failure(Exception("Сервер не отвечает. Попробуйте позже."))

    } catch (e: Exception) {
        println("❌ [Network] Неизвестная ошибка")
        println("   Класс: ${e.javaClass.name}")
        println("   Сообщение: ${e.message}")
        println("   Stack trace:")
        e.printStackTrace()

        // ✅ Не показываем техническую информацию пользователю
        val message = when {
            e.message?.contains("Expected response body") == true ->
                "Сервер вернул некорректный ответ"

            else -> "Не удалось выполнить запрос"
        }

        Result.failure(Exception(message))
    }
}
