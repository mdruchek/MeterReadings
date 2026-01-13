// app/src/main/java/ru/dr/meterreadings/data/mappers/KvcMeterMapper.kt

package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
import ru.dr.meterreadings.models.ui.MeterUiModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Маппер для преобразования счётчиков КВЦ из API в UI модели
 */
object KvcMeterMapper {

    /**
     * Преобразует счётчик КВЦ из API в UI модель
     *
     * @param kvcCounter - счётчик из KVC API
     * @param accountId - ID аккаунта (лицевой счёт)
     * @return UI модель для отображения
     */
    fun mapToUi(
        kvcCounter: KvcCounterDto,
        accountId: String
    ): MeterUiModel {
        return MeterUiModel(
            // Уникальный ID: account_counterid
            id = "${accountId}_${kvcCounter.idCnt}",

            // Привязываем к аккаунту
            accountId = accountId,

            // Тип счётчика
            // Для двухтарифных добавляем "(день/ночь)"
            type = if (kvcCounter.idTtype == "2T") {
                "${kvcCounter.servName.trim()} (${kvcCounter.idTtype})"
            } else {
                kvcCounter.servName.trim()
            },

            // Заводской номер
            serialNumber = kvcCounter.number.trim(),

            // Последнее показание (целое число)
            lastValue = parseValueAsInt(kvcCounter.cValLst),

            // Дата последней передачи
            lastUpdateDate = parseDate(kvcCounter.datLst)
        )
    }

    /**
     * Преобразует список счётчиков КВЦ
     */
    fun mapListToUi(
        kvcCounters: List<KvcCounterDto>,
        accountId: String
    ): List<MeterUiModel> {
        return kvcCounters
            .filter { it.canEdit() }  // Только редактируемые
            .map { mapToUi(it, accountId) }
    }

    /**
     * Парсит значение показания как целое число
     *
     * КВЦ API возвращает строки типа:
     * - "123" → 123
     * - "123.45" → 123 (отбрасываем дробную часть)
     * - "123,45" → 123 (отбрасываем дробную часть)
     * - "0" → null
     * - "" → null
     *
     * @param value строка показания
     * @return Int или null если не удалось распарсить
     */
    private fun parseValueAsInt(value: String): Int? {
        if (value.isBlank() || value == "0") return null

        return try {
            // Убираем дробную часть если есть
            val cleaned = value.trim().replace(",", ".")
            val doubleValue = cleaned.toDoubleOrNull()
            doubleValue?.toInt()  // ✅ Преобразуем Double → Int
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Парсит дату из строки КВЦ в timestamp
     *
     * Поддерживаемые форматы:
     * - "2025-01-12T00:00:00" (ISO с временем)
     * - "2025-01-12" (ISO)
     * - "12.01.2025" (российский)
     *
     * @param dateString строка даты
     * @return timestamp в миллисекундах или null
     */
    private fun parseDate(dateString: String): Long? {
        if (dateString.isBlank()) return null

        // Список форматов для попытки парсинга
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd.MM.yyyy", Locale("ru")),
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
        )

        // Пробуем каждый формат
        for (format in formats) {
            try {
                return format.parse(dateString.trim())?.time
            } catch (e: Exception) {
                // Продолжаем со следующим форматом
                continue
            }
        }

        // Не удалось распарсить
        return null
    }
}
