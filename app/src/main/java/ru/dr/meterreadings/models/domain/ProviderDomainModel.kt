package ru.dr.meterreadings.models.domain

data class ProviderDomainModel(
    val id: String,
    val name: String,
    val type: Type,
    val logoUrl: String? = null,   // URL
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
    val lastAutoUpdate: Long? = null,              // ✨ НОВОЕ

    // ============================================
    // НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================
    val updateNotificationsEnabled: Boolean = true,
    val errorNotificationsEnabled: Boolean = true, // ✨ НОВОЕ

    // ============================================
    // НАСТРОЙКИ НАПОМИНАНИЙ
    // ============================================
    val reminderEnabled: Boolean = true,
    val reminderTimeHour: Int = 9,
    val reminderTimeMinute: Int = 0,
    val reminderPeriodMode: String = "AUTO",       // ✨ НОВОЕ: "AUTO" или "MANUAL"
    val reminderCustomStartDay: Int? = null,       // ✨ НОВОЕ
    val reminderCustomEndDay: Int? = null          // ✨ НОВОЕ
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
