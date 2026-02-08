package ru.dr.meterreadings.utils

import ru.dr.meterreadings.domain.connector.AuthException
import ru.dr.meterreadings.domain.service.AuthService
import ru.dr.meterreadings.domain.service.TokenResult
import ru.dr.meterreadings.models.ui.AuthError

/**
 * Обёртка для сетевых запросов с автоматической проверкой токенов
 *
 * Комбинирует проверку авторизации + обработку HTTP ошибок.
 *
 * @param authService Сервис авторизации
 * @param providerId ID провайдера
 * @param regionId ID региона (для передачи в ошибку)
 * @param block Блок с сетевым запросом, принимает accessToken
 * @return Result с данными или AuthException/NetworkException
 */
suspend fun <T> safeAuthenticatedCall(
    authService: AuthService,
    providerId: Long,
    regionId: String? = null,
    block: suspend (accessToken: String) -> T
): Result<T> {
    return try {
        // ============================================
        // 1️⃣ ПРОВЕРКА АВТОРИЗАЦИИ
        // ============================================

        // Получаем всех авторизованных пользователей
        val users = authService.getAuthorizedUsers(providerId)

        if (users.isEmpty()) {
            throw AuthException(AuthError.NotAuthorized(providerId, regionId))
        }

        // Берём первого пользователя
        val login = users.first().login

        // Получаем валидный токен
        val tokenResult = authService.getValidAccessToken(providerId, login, regionId)

        val accessToken = when (tokenResult) {
            is TokenResult.Success -> tokenResult.accessToken
            is TokenResult.Error -> {
                throw AuthException(tokenResult.authError)
            }
        }

        // ============================================
        // 2️⃣ ВЫПОЛНЕНИЕ ЗАПРОСА С ОБРАБОТКОЙ HTTP ОШИБОК
        // ============================================

        safeNetworkCall {
            block(accessToken)
        }

    } catch (e: AuthException) {
        // Пробрасываем ошибки авторизации как есть
        Result.failure(e)
    } catch (e: Exception) {
        // Остальные ошибки (уже обработаны в safeNetworkCall)
        Result.failure(e)
    }
}

/**
 * Вариант для конкретного логина
 */
suspend fun <T>     safeAuthenticatedCall(
    authService: AuthService,
    providerId: Long,
    login: String,
    regionId: String? = null,
    block: suspend (accessToken: String) -> T
): Result<T> {
    return try {
        // Получаем валидный токен для конкретного логина
        val tokenResult = authService.getValidAccessToken(providerId, login, regionId)

        val accessToken = when (tokenResult) {
            is TokenResult.Success -> tokenResult.accessToken
            is TokenResult.Error -> {
                throw AuthException(tokenResult.authError)
            }
        }

        // Выполняем запрос с обработкой HTTP ошибок
        safeNetworkCall {
            block(accessToken)
        }

    } catch (e: AuthException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
