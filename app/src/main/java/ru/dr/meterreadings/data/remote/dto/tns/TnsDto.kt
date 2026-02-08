package ru.dr.meterreadings.data.remote.dto.tns

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =====================================================
// Авторизация приложения
// =====================================================
@Serializable
data class TnsAppVersionResponse(
    @SerialName("result")
    val result: Boolean,
    @SerialName("statusCode")
    val statusCode: Int,
    @SerialName("data")
    val data: TnsAppVersionData
)

@Serializable
data class TnsAppVersionData(
    @SerialName("status")
    val status: Int
)

// =====================================================
// Регионы
// =====================================================
@Serializable
data class TnsRegionsResponse(
    @SerialName("result")
    val result: Boolean,
    @SerialName("statusCode")
    val statusCode: Int,
    @SerialName("data")  // ✅ БЫЛО: regions
    val data: List<TnsRegionDto>
)

@Serializable
data class TnsRegionDto(
    @SerialName("name")
    val name: String,
    @SerialName("code")
    val code: String,
    @SerialName("coordinates")
    val coordinates: String
)

// =====================================================
// Авторизация пользователя
// =====================================================
@Serializable
data class TnsUserAuthRequest(
    @SerialName("login")
    val login: String,
    @SerialName("authType")
    val authType: String,
    @SerialName("password")
    val password: String,
    @SerialName("region")
    val region: String,
    @SerialName("platform")
    val platform: String
)

@Serializable
data class TnsUserAuthResponse(
    @SerialName("result")
    val result: Boolean? = null,      // ✅ Nullable на случай ошибки
    @SerialName("statusCode")
    val statusCode: Int? = null,      // ✅ Nullable на случай ошибки
    @SerialName("data")
    val data: TnsAuthData? = null,
    @SerialName("success")
    val success: Boolean? = null,     // ✅ Добавить @SerialName
    @SerialName("message")
    val message: String? = null,      // ✅ Добавить @SerialName
    @SerialName("errors")
    val errors: List<TnsError>? = null  // ✅ Добавить @SerialName
)

@Serializable
data class TnsAuthData(
    @SerialName("accessTokenExpires")
    val accessTokenExpires: String,
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshTokenExpires")
    val refreshTokenExpires: String,
    @SerialName("refreshToken")
    val refreshToken: String
)

@Serializable
data class TnsError(
    @SerialName("field")
    val field: String? = null,
    @SerialName("message")
    val message: String? = null
)

// =====================================================
// Получение лицевых счетов
// =====================================================

@Serializable
data class TnsAccountsResponse(
    @SerialName("result")
    val result: Boolean,

    @SerialName("statusCode")
    val statusCode: Int,

    @SerialName("data")
    val data: List<TnsAccountDto>
)

@Serializable
data class TnsAccountDto(
    @SerialName("id")
    val id: Int,

    @SerialName("number")
    val number: String,

    @SerialName("name")
    val name: String?,

    @SerialName("address")
    val address: String,

    @SerialName("isueAvaliable")
    val isueAvailable: Boolean,

    @SerialName("initial_year")
    val initialYear: Int
)

// =====================================================
// Получение счётчиков аккаунта
// =====================================================

/**
 * Ответ на запрос счётчиков
 * GET /api/v1/counters?account=521041038358
 */
@Serializable
data class TnsCountersResponse(
    @SerialName("result")
    val result: Boolean,

    @SerialName("statusCode")
    val statusCode: Int,

    @SerialName("data")
    val data: List<TnsCounterDto>
)

/**
 * Счётчик (прибор учёта)
 */
@Serializable
data class TnsCounterDto(
    @SerialName("counterId")
    val counterId: String,          // "8574750"

    @SerialName("rowId")
    val rowId: String,              // "3878906"

    @SerialName("installationType")
    val installationType: String,   // "" (может быть пустым)

    @SerialName("tariff")
    val tariff: Int,                // 1 (однотарифный) или 2 (двухтарифный)

    @SerialName("checkingDate")
    val checkingDate: String,       // "01.01.2027" (дата поверки)

    @SerialName("lastReadings")
    val lastReadings: List<TnsLastReadingDto>
)

/**
 * Последнее показание счётчика
 */
@Serializable
data class TnsLastReadingDto(
    @SerialName("name")
    val name: String,               // "День" / "Ночь" / "Т1" / "Т2"

    @SerialName("value")
    val value: String,              // "9945" (последнее показание)

    @SerialName("date")
    val date: String                // "25.01.26" (дата последнего показания)
)

