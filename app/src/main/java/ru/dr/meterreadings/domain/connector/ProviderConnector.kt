package ru.dr.meterreadings.domain.connector

import kotlin.coroutines.RestrictsSuspension

/**
 * Базовый интерфейс для всех провайдеров
 */
interface ProviderConnector {
    val providerId: Long
    val providerName: String
}

// ============================================
// Capabilities
// ============================================

/**
 * Провайдер требует авторизации приложения перед работой
 */
interface AppAuth {
    suspend fun appAuth(): Result<Boolean>
}

interface UserAuth {
    /**
     * Авторизация пользователя
     *
     * @param login Логин
     * @param password Пароль
     * @param regionId ID региона (опционально)
     * @return Result с данными авторизации (токены)
     */
    suspend fun userAuth(
        login: String,
        password: String,
        regionId: String? = null
    ): Result<UserAuthData>

    data class UserAuthData(
        val authSuccess: Boolean,
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val accessTokenExpires: String? = null,
        val refreshTokenExpires: String? = null
    )
}


/**
 * Провайдер имеет региональное деление
 */
interface GetRegions {
    suspend fun getRegions(): Result<List<RegionInfo>>

    data class RegionInfo(
        val id: String,
        val name: String
    )
}

/**
 * Поиск лицевого счёта
 *
 * Возвращает адрес для подтверждения перед сохранением.
 */
interface GetAccounts {
    /**
     * Поиск лицевых счетов по номеру
     */
    suspend fun getAccounts(
        accountNumber: String,
        regionId: String? = null,
    ): Result<List<AccountInfo>>

    data class AccountInfo(
        val accountNumber: String,
        val address: String? = null,
        val regionId: String? = null
    )
}

/**
 * Передача показаний счётчиков онлайн
 */
interface SubmitReadings {
    suspend fun submitReading(
        counterId: String,
        accountNumber: String,
        value: String,
        valueNight: String? = null,
        regionId: String? = null,
        cacheData: Any? = null
    ): Result<Unit>
}

/**
 * Провайдер поддерживает получение периода передачи показаний
 */
interface GetTransmissionPeriod {
    suspend fun getTransmissionPeriod(
        accountNumber: String,
        regionId: Int? = null
    ): Result<TransmissionPeriod>

    data class TransmissionPeriod(
        val startDay: Int,  // День начала (1-31)
        val endDay: Int     // День окончания (1-31)
    )
}

/**
 * Провайдер поддерживает загрузку счётчиков
 */
interface GetMeters {
    suspend fun getMeters(
        accountNumber: String,
        regionId: String? = null
    ): Result<GetMetersResult>

    data class GetMetersResult(
        val meters: List<MeterInfo>,
        val cacheData: Any? = null  // Опционально для кеша (например, KvcLocationDto)
    )

    data class MeterInfo(
        val id: String,
        val type: String,
        val serialNumber: String,
        val lastValue: Int?,
        val lastSubmissionDate: String?,
        val apiCounterId: Int
    )
}

/**
 * Провайдер поддерживает валидацию показаний перед отправкой
 */
interface ValidateReading {
    /**
     * Получить минимально допустимое показание для счётчика
     * (обычно это показание предыдущего месяца)
     */
    suspend fun getMinimumAllowedValue(
        counterId: String,
        accountNumber: String,
        regionId: String?,
        cacheData: Any? = null
    ): Result<Int?>
}

/**
 * Провайдер поддерживает получение истории показаний
 */
interface GetMeterHistory {
    /**
     * История одного показания за месяц
     */
    data class MeterHistory(
        /** Месяц (1-12) */
        val month: Int,

        /** Год (2025, 2026...) */
        val year: Int,

        /** Показание на конец месяца */
        val value: Int,

        /** Расход за месяц (разница) */
        val consumption: Int
    )

    /**
     * Получить историю показаний счётчика
     *
     * @param counterId ID счётчика в API провайдера
     * @param accountNumber Номер лицевого счёта
     * @param regionId ID региона (если требуется)
     * @param cacheData Кешированные данные для оптимизации
     * @return Result со списком истории (от новых к старым)
     */
    suspend fun getMeterHistory(
        counterId: String,
        accountNumber: String,
        regionId: String? = null,
        cacheData: Any? = null
    ): Result<List<MeterHistory>>
}

// ============================================
// Типы ресурсов
// ============================================

enum class ResourceType {
    COLD_WATER,
    HOT_WATER,
    ELECTRICITY
}
