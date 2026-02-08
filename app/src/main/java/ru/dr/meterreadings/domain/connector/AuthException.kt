package ru.dr.meterreadings.domain.connector

import ru.dr.meterreadings.models.ui.AuthError

/**
 * Исключение для ошибок авторизации
 *
 * Бросается когда токен истёк, не найден или refresh не удался.
 * Содержит структурированную информацию об ошибке для UI.
 */
class AuthException(val authError: AuthError) : Exception(authError.message)
