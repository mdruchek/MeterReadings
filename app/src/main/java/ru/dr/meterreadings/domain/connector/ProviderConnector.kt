package ru.dr.meterreadings.domain.connector

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
     *
     * @param accountNumber Номер лицевого счёта
     * @param regionId ID региона (опционально)
     * @param login Логин пользователя (для ТНС обязателен)
     */
    suspend fun getAccounts(
        accountNumber: String,
        regionId: String? = null,
        login: String? = null
    ): Result<List<AccountInfo>>

    data class AccountInfo(
        val number: String,
        val uuid: String? = null,
        val address: String? = null,
        val regionId: String? = null,
        val login: String? = null,
        val submissionStartDay: Int?,
        val submissionEndDay: Int?,
        val additionalInfo: String?   // Любая дополнительная информация
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
        regionId: String? = null
    ): Result<Unit>
}

/**
 * Провайдер поддерживает получение периода передачи показаний
 */
interface GetTransmissionPeriod {
    suspend fun getTransmissionPeriod(
        accountNumber: String,
        regionId: String? = null
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
        regionId: String? = null,
        apiAccountId: String? = null
    ): Result<GetMetersResult>

    data class GetMetersResult(
        val meters: List<MeterInfo>,
    )

    data class MeterInfo(
        val id: String,
        val number: String,
        val lastFirstValue: Int?,
        val lastSecondValue: Int?,
        val lastThirdValue: Int?,
        val type: String?,
        val verificationDate: String?,
        val maxDiff: Int?,
        val apiAccountId: String?, //api abonent uuid
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
        regionId: String?
    ): Result<Int?>
}

/**
 * Провайдер поддерживает получение истории показаний
 */
interface GetMeterHistory {
    /**
     * Получить историю показаний счётчика
     *
     * @param meterId ID счётчика в API провайдера
     * @param regionId ID региона (если требуется)
     * @return Result со списком истории (от новых к старым)
     */
    suspend fun getMeterHistory(
        meterNumber: String? = null,
        meterId: String? = null,
        regionId: String? = null
    ): Result<List<MeterHistoryInfo>>

    data class MeterHistoryInfo(
        /** Месяц (1-12) */
        val month: Int,
        /** Год (2025, 2026...) */
        val year: Int,
        val tariffs: List<TariffInfo>,
    )

    data class TariffInfo(
        val indicationType: String,  // "Пик", "Ночь", "Общий", "T1", "T2"...
        val lastValue: Int,
        val prevValue: Int,
        val consumption: Int
    )
}

// ============================================
// Типы ресурсов
// ============================================

enum class ResourceType {
    COLD_WATER,
    HOT_WATER,
    ELECTRICITY
}
