package ru.dr.meterreadings.models.domain

data class ProviderDomainModel(
    val id: String,
    val name: String,
    val type: Type,
    val logoUrl: String?,   // URL
    val baseUrl: String,
    val authType: AuthType,

    // ============================================
    // ПЕРИОД ПЕРЕДАЧИ ПОКАЗАНИЙ
    // ============================================
    val transmissionPeriodStartDay: Int?,
    val transmissionPeriodEndDay: Int?,
    val lastPeriodUpdate: Long?,
    val periodLoadedForMonth: String?,

    // ============================================
    // НАСТРОЙКИ АВТООБНОВЛЕНИЯ
    // ============================================
    val autoUpdateEnabled: Boolean,
    val updateStartDay: Int,
    val updateIntervalHours: Int,
    val lastAutoUpdate: Long?,              // ✨ НОВОЕ

    // ============================================
    // НАСТРОЙКИ УВЕДОМЛЕНИЙ
    // ============================================
    val notificationsEnabled: Boolean,

    // ============================================
    // НАСТРОЙКИ НАПОМИНАНИЙ
    // ============================================
    val reminderEnabled: Boolean,
    val reminderTimeHour: Int,
    val reminderTimeMinute: Int,
    val reminderPeriodMode: String,       // ✨ НОВОЕ: "AUTO" или "MANUAL"
    val reminderCustomStartDay: Int?,       // ✨ НОВОЕ
    val reminderCustomEndDay: Int?        // ✨ НОВОЕ
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
