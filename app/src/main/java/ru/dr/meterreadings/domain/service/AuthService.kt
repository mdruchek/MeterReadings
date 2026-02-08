package ru.dr.meterreadings.domain.service

import ru.dr.meterreadings.data.local.TokenManager
import ru.dr.meterreadings.models.domain.AuthTokenDomainModel
import ru.dr.meterreadings.models.ui.AuthError
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Результат получения токена
 */
sealed class TokenResult {
    data class Success(val accessToken: String) : TokenResult()
    data class Error(val authError: AuthError) : TokenResult()
}

@Singleton
class AuthService @Inject constructor(
    private val tokenManager: TokenManager
) {

    /**
     * Получить валидный access token для конкретного логина
     *
     * @param providerId ID провайдера
     * @param login Логин пользователя
     * @param regionId Регион (для передачи в ошибку)
     * @return TokenResult с токеном или ошибкой
     */
    fun getValidAccessToken(
        providerId: Long,
        login: String,
        regionId: String? = null
    ): TokenResult {
        println("🔍 [AuthService] Запрос токена для $login (providerId=$providerId)")
        val token = tokenManager.getToken(providerId, login)

        if (token == null) {
            println("❌ [AuthService] Токен не найден для $login")
            return TokenResult.Error(
                AuthError.NotAuthorized(providerId, regionId)
            )
        }

        // 2️⃣ Проверяем, истёк ли refresh token (полное истечение)
        if (tokenManager.isRefreshTokenExpired(token)) {
            println("❌ [AuthService] Refresh token истёк для $login")
            tokenManager.deleteToken(providerId, login)
            return TokenResult.Error(
                AuthError.NotAuthorized(providerId, regionId)
            )
        }

        // 3️⃣ Проверяем, истёк ли access token
        if (tokenManager.isAccessTokenExpired(token)) {
            println("⚠️ [AuthService] Access token истёк для $login")

            // TODO: Здесь будет автоматический refresh
            // val refreshResult = refreshToken(token, regionCode)
            // if (refreshResult.isSuccess) return TokenResult.Success(...)

            // ВРЕМЕННО: возвращаем ошибку с просьбой повторной авторизации
            return TokenResult.Error(
                AuthError.TokenExpiredNoRefresh(
                    providerId = providerId,
                    login = login,
                    regionId = regionId
                )
            )
        }

        // 4️⃣ Токен валидный
        println("✅ [AuthService] Токен валидный для $login")
        return TokenResult.Success(token.accessToken)
    }

    /**
     * Получить токен (без проверки)
     */
    fun getToken(providerId: Long, login: String): AuthTokenDomainModel? {
        return tokenManager.getToken(providerId, login)
    }

    /**
     * Сохранить токен
     */
    fun saveToken(token: AuthTokenDomainModel) {
        tokenManager.saveToken(token)
    }

    /**
     * Проверить, авторизован ли пользователь (refresh token валиден)
     */
    fun isAuthorized(providerId: Long, login: String): Boolean {
        val token = tokenManager.getToken(providerId, login) ?: return false
        return !tokenManager.isRefreshTokenExpired(token)
    }

    /**
     * Получить всех авторизованных пользователей провайдера
     */
    fun getAuthorizedUsers(providerId: Long): List<AuthTokenDomainModel> {
        return tokenManager.getAllTokensForProvider(providerId)
            .filter { !tokenManager.isRefreshTokenExpired(it) }
    }

    /**
     * Удалить токен (выход)
     */
    fun logout(providerId: Long, login: String) {
        tokenManager.deleteToken(providerId, login)
    }
}
