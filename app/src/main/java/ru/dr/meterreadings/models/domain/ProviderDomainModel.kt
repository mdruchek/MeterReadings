package ru.dr.meterreadings.models.domain

data class ProviderDomainModel(
    val id: Long,
    val name: String,
    val type: Type,
    val logoUrl: String? = null,
    val baseUrl: String,
    val authType: AuthType,

    // ============================================
    // ПЕРИОД ПЕРЕДАЧИ ПОКАЗАНИЙ
    // ============================================
    val transmissionPeriodStartDay: Int? = null,
    val transmissionPeriodEndDay: Int? = null,
    val lastPeriodUpdate: Long? = null,
    val periodLoadedForMonth: String? = null,

    // ============================================
    // НАСТРОЙКИ АВТООБНОВЛЕНИЯ
    // ============================================
    val autoUpdateEnabled: Boolean = false,

    // ============================================
    // НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================
    val notificationsEnabled: Boolean = false,

    // ============================================
    // НАСТРОЙКИ НАПОМИНАНИЙ
    // ============================================
    val reminderEnabled: Boolean = false,
    val reminderCustomStartDay: Int? = null
)

enum class AuthType {
    ACCOUNT_NUMBER,
    API_KEY,
    FORM_CSRF,
    AUTH_REQUIRED
}

enum class Type {
    WaterSupply,
    ElectricitySupply,
    GasSupply
}
