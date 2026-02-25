package ru.dr.meterreadings.models.domain

import ru.dr.meterreadings.domain.connector.GetMeters
import ru.dr.meterreadings.models.ui.MeterUiModel

data class MeterDomainModel(
    // ========================================
    // ID и связи
    // ========================================
    val id: String,
    val apiId: String,
    val accountId: String,       // ID аккаунта в БД (для связи)
    val apiAccountId: String,
    val providerId: Long,        // ID провайдера

    // ========================================
    // Данные счётчика (из API)
    // ========================================
    val number: String,          // Номер счётчика
    val type: String?,           // Тип (Электроснабжение, ХВС, ГВС)
    val verificationDate: String?, // Дата поверки
    val maxDiff: Int?,           // Макс. разница

    // ========================================
    // Показания (из API)
    // ========================================
    val lastFirstValue: Int?,    // Показание Т1
    val lastSecondValue: Int?,   // Показание Т2
    val lastThirdValue: Int?,    // Показание Т3
    val lastSubmissionDate: String?, // Дата последней подачи
)

fun GetMeters.MeterInfo.toMeterDomainModel(accountId: String, providerId: Long) = MeterDomainModel(
    id = id,
    apiId = id,
    accountId = accountId,
    apiAccountId = apiAccountId ?: "",
    providerId = providerId,
    number = number,
    type = type,
    verificationDate = verificationDate,
    maxDiff = maxDiff,
    lastFirstValue = lastFirstValue,
    lastSecondValue = lastSecondValue,
    lastThirdValue = lastThirdValue,
    lastSubmissionDate = null,
)

fun MeterDomainModel.toUiModel(): MeterUiModel = MeterUiModel(
    id = id,
    accountId = accountId,
    type = type ?: "Неизвестно",
    number = number,
    lastFirstValue = lastFirstValue,
    lastSecondValue = lastSecondValue,
    lastThirdValue = lastThirdValue,
    lastSubmissionDate = lastSubmissionDate,
    lastMonthConsumption = null,   // вычисляемое поле — пока null
    verificationDate = verificationDate,
)

