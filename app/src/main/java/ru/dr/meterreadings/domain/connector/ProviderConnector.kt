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
 * Провайдер имеет региональное деление
 */
interface HasRegions {
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
interface SearchAccount {
    suspend fun searchAccount(
        accountNumber: String,
        regionId: String? = null
    ): Result<String>
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
interface LoadMeters {
    suspend fun loadMeters(
        accountNumber: String,
        regionId: String? = null
    ): Result<LoadMetersResult>

    data class LoadMetersResult(
        val meters: List<MeterInfo>,
        val address: String,
        val cacheData: Any? = null  // Опционально для кеша (например, KvcLocationDto)
    )

    data class MeterInfo(
        val id: String,
        val type: String,
        val serialNumber: String,
        val lastValue: Int?,
        val lastUpdateTimestamp: Long?,
        val lastSubmissionDate: String?,
        val apiCounterId: Int
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
