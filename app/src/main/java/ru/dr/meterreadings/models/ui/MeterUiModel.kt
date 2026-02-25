package ru.dr.meterreadings.models.ui

/**
 * UI модель счетчика
 *
 * Представляет счётчик любого типа (вода, электричество, газ и т.д.)
 * для отображения в интерфейсе.
 */
data class MeterUiModel(
    /** Уникальный идентификатор счётчика */
    val id: String, //точно нужен в ui?
    /** ID аккаунта, к которому привязан счётчик */
    val accountId: String,
    /** Тип счётчика: "Холодная вода", "День", "T1" и т.д. */
    val type: String,
    /** Заводской номер счётчика */
    val number: String,
    /** Последнее переданное показание первого тарифа (с сайта) */
    val lastFirstValue: Int?,
    /** Последнее переданное показание первого тарифа (с сайта) */
    val lastSecondValue: Int?,
    /** Последнее переданное показание первого тарифа (с сайта) */
    val lastThirdValue: Int?,
    /** Дата последней передачи показаний с api */
    val lastSubmissionDate: String?,
    /** объём потребления за последний месяц */
    val lastMonthConsumption: Int? = null,
    /** дата поверки*/
    val verificationDate: String?,
)
