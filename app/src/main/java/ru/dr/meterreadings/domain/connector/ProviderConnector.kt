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
        regionId: String? = null
    ): Result<Unit>
}

// ============================================
// Типы ресурсов
// ============================================

enum class ResourceType {
    COLD_WATER,
    HOT_WATER,
    ELECTRICITY
}
