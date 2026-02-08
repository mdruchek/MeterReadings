package ru.dr.meterreadings.models.domain

import kotlinx.serialization.Serializable

/**
 * Токен авторизации для конкретного логина в конкретном провайдере
 */
@Serializable
data class AuthTokenDomainModel(
    val providerId: Long,            // ID провайдера
    val login: String,               // Логин пользователя
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Long,
    val refreshTokenExpiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)
