package ru.dr.meterreadings.domain.constants

/**
 * Константы ID провайдеров
 *
 * Используются для:
 * - Инициализации БД
 * - Получения коннектора через фабрику
 * - Миграций
 */
object ProviderIds {
    const val KVC = 1L
    const val TNS = 2L
}
