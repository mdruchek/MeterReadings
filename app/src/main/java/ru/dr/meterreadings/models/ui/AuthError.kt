package ru.dr.meterreadings.models.ui

/**
 * Ошибки авторизации для UI
 */
sealed class AuthError(
    val title: String,
    val message: String
) {
    /**
     * Токен истёк, refresh не реализован
     */
    data class TokenExpiredNoRefresh(
        val providerId: Long,
        val login: String,
        val regionId: String?
    ) : AuthError(
        title = "Токен истёк",
        message = "Требуется повторная авторизация.\n\nАвтоматическое обновление токенов ещё не реализовано."
    )

    /**
     * Токен истёк и refresh failed
     */
    data class RefreshFailed(
        val providerId: Long,
        val login: String,
        val regionId: String?,
        val errorMessage: String
    ) : AuthError(
        title = "Не удалось обновить токен",
        message = "Требуется повторная авторизация.\n\nОшибка: $errorMessage"
    )

    /**
     * Пользователь не авторизован
     */
    data class NotAuthorized(
        val providerId: Long,
        val regionId: String?
    ) : AuthError(
        title = "Требуется авторизация",
        message = "Войдите в систему для продолжения работы."
    )
}
