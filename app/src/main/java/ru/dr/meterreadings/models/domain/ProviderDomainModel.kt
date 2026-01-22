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
    val updateStartDay: Int = 1,
    val updateIntervalHours: Int = 1,
    val lastAutoUpdate: Long? = null,

    // ============================================
    // НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================
    val notificationsEnabled: Boolean = false,

    // ============================================
    // НАСТРОЙКИ НАПОМИНАНИЙ
    // ============================================
    val reminderEnabled: Boolean = false,
    val reminderTimeHour: Int = 9,
    val reminderTimeMinute: Int = 0,
    val reminderPeriodMode: String = "AUTO",
    val reminderCustomStartDay: Int? = null,
    val reminderCustomEndDay: Int? = null
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
