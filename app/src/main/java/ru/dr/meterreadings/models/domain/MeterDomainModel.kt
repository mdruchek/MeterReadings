package ru.dr.meterreadings.models.domain

data class MeterDomainModel(
    val id: String,
    val accountId: String,
    val type: MeterType,
    val serialNumber: String,
    val location: String? = null,
    val lastReading: Double? = null,
    val lastReadingDate: Long? = null
)

enum class MeterType {
    WATER_COLD,
    WATER_HOT,
    GAS,
    ELECTRICITY
}
