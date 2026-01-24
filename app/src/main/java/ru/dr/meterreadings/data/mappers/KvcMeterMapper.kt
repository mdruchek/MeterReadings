//// app/src/main/java/ru/dr/meterreadings/data/mappers/KvcMeterMapper.kt
//
//package ru.dr.meterreadings.data.mappers
//
//import ru.dr.meterreadings.data.local.entities.MeterEntity
//import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
//import ru.dr.meterreadings.models.ui.MeterUiModel
//import java.text.SimpleDateFormat
//import java.util.Locale
//
///**
// * Маппер для преобразования счётчиков КВЦ
// */
//object KvcMeterMapper {
//
//    // ========================================
//    // МАППИНГ В UI (для отображения)
//    // ========================================
//
//    /**
//     * Преобразует счётчик КВЦ в UI модель для отображения
//     */
//    fun mapToUi(
//        kvcCounter: KvcCounterDto,
//        accountId: String
//    ): MeterUiModel {
//        return MeterUiModel(
//            id = "${accountId}_${kvcCounter.idCnt}",
//            accountId = accountId,
//            type = if (kvcCounter.idTtype == "2T") {
//                "${kvcCounter.servName.trim()} (${kvcCounter.idTtype})"
//            } else {
//                kvcCounter.servName.trim()
//            },
//            serialNumber = kvcCounter.number.trim(),
//            lastValue = parseValueAsInt(kvcCounter.cValLst),
//            lastSubmissionDate = parseTimestamp(kvcCounter.datB)
//        )
//    }
//
//    /**
//     * Преобразует список счётчиков КВЦ в UI модели
//     */
//    fun mapListToUi(
//        kvcCounters: List<KvcCounterDto>,
//        accountId: String
//    ): List<MeterUiModel> {
//        return kvcCounters
//            .filter { it.canEdit() }
//            .map { mapToUi(it, accountId) }
//    }
//
//    // ========================================
//    // МАППИНГ В ENTITY (для БД и уведомлений)
//    // ========================================
//
//    /**
//     * Преобразует список счётчиков КВЦ в Entity для сохранения в БД
//     */
//    fun mapListToEntity(
//        kvcCounters: List<KvcCounterDto>,
//        accountId: String
//    ): List<MeterEntity> {
//        return kvcCounters
//            .filter { it.canEdit() }
//            .map { kvcCounter ->
//                MeterEntity(
//                    id = "${accountId}_${kvcCounter.idCnt}",
//                    accountId = accountId,
//                    apiCounterId = kvcCounter.idCnt,
//                    type = if (kvcCounter.idTtype == "2T") {  // ✅ ДОБАВЛЕНО
//                        "${kvcCounter.servName.trim()} (${kvcCounter.idTtype})"
//                    } else {
//                        kvcCounter.servName.trim()
//                    },
//                    serialNumber = kvcCounter.number.trim(),  // ✅ ДОБАВЛЕНО
//                    lastSubmissionDate = parseSubmissionDate(kvcCounter.datLst)
//                )
//            }
//    }
//
//    // ========================================
//    // ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
//    // ========================================
//
//    /**
//     * Парсит показание как целое число
//     */
//    private fun parseValueAsInt(value: String): Int? {
//        if (value.isBlank() || value == "0") return null
//        return try {
//            val cleaned = value.trim().replace(",", ".")
//            cleaned.toDoubleOrNull()?.toInt()
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    /**
//     * Парсит дату в timestamp (для UI)
//     */
//    private fun parseTimestamp(dateString: String): String? {
//        if (dateString.isBlank()) return null
//
//        val formats = listOf(
//            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
//            SimpleDateFormat("yyyy-MM-dd", Locale.US),
//            SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
//        )
//
//        for (format in formats) {
//            try {
//                return format.parse(dateString.trim())?.time
//            } catch (e: Exception) {
//                continue
//            }
//        }
//        return null
//    }
//
//    /**
//     * Парсит дату в формат dd.MM.yyyy (для Entity)
//     */
//    private fun parseSubmissionDate(dateString: String): String? {
//        if (dateString.isBlank()) return null
//
//        return try {
//            val datePart = dateString.substringBefore("T")
//            val parts = datePart.split("-")
//            if (parts.size == 3) {
//                val year = parts[0]
//                val month = parts[1]
//                val day = parts[2]
//                "$day.$month.$year"
//            } else {
//                null
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
//}
