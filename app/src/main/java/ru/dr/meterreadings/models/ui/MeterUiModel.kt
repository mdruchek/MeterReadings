package ru.dr.meterreadings.models.ui

/**
 * UI модель счетчика
 *
 * Представляет счётчик любого типа (вода, электричество, газ и т.д.)
 * для отображения в интерфейсе.
 */
data class MeterUiModel(
    /** Уникальный идентификатор счётчика */
    val id: String,
    /** ID аккаунта, к которому привязан счётчик */
    val accountId: String,
    /** Тип счётчика: "Холодная вода", "День", "T1" и т.д. */
    val type: String,
    /** Заводской номер счётчика */
    val serialNumber: String,
    /** Последнее переданное показание (с сайта) */
    val lastValue: Int?,
    /** Дата последней передачи показаний с api */
    val lastSubmissionDate: String?,
    /** объём потребления за последний месяц */
    val lastMonthConsumption: Int? = null
)
