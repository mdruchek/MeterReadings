// app/src/main/java/ru/dr/meterreadings/data/local/models/MeterWithAccount.kt

package ru.dr.meterreadings.data.local.models

/**
 * Результат JOIN meters + accounts
 * Используется в Worker для получения счётчиков с данными аккаунта
 */
data class MeterWithAccount(
    val id: String,
    val accountId: String,
    val apiCounterId: Int,
    val lastSubmissionDate: String?,

    // Из таблицы accounts
    val accountNumber: String,
    val providerId: String
)
