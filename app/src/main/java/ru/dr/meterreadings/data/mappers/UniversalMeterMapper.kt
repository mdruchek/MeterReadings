//package ru.dr.meterreadings.data.mappers
//
//import ru.dr.meterreadings.data.local.entities.MeterEntity
//import ru.dr.meterreadings.data.remote.dto.tns.TnsCounterDto
//import ru.dr.meterreadings.domain.connector.GetMeters
//import ru.dr.meterreadings.models.ui.MeterUiModel
//
///**
// * Универсальный маппер для счётчиков от любого провайдера
// */
//object UniversalMeterMapper {
//
//    // =====================================================
//    // ТНС МАППИНГ
//    // =====================================================
//
//    /**
//     * Маппинг TnsCounterDto → GetMeters.MeterInfo
//     *
//     * @param dto DTO счётчика от ТНС
//     * @return Универсальная модель MeterInfo
//     */
//    fun fromTnsDto(dto: TnsCounterDto): GetMeters.MeterInfo {
//        // Получаем последнее показание (обычно первое в списке)
//        val lastReading = dto.lastReadings.firstOrNull()
//
//        return GetMeters.MeterInfo(
//            id = dto.counterId,                              // "8574750"
//            type = mapTnsTariffToType(dto.tariff),          // "Однотарифный" / "Двухтарифный"
//            number = dto.rowId,                        // "3878906" (используем rowId как серийник)
//            lastValue = lastReading?.value?.toIntOrNull(),   // 9945
//            lastSubmissionDate = lastReading?.date,          // "25.01.26"
//            apiCounterId = dto.counterId.toIntOrNull() ?: 0  // 8574750
//        )
//    }
//
//    /**
//     * Маппинг списка TnsCounterDto → List<MeterInfo>
//     */
//    fun fromTnsDtoList(dtoList: List<TnsCounterDto>): List<GetMeters.MeterInfo> {
//        return dtoList.map { fromTnsDto(it) }
//    }
//
//    /**
//     * Преобразует тариф ТНС в читаемый тип
//     *
//     * @param tariff 1 = однотарифный, 2 = двухтарифный
//     */
//    private fun mapTnsTariffToType(tariff: Int): String {
//        return when (tariff) {
//            1 -> "Электричество (однотарифный)"
//            2 -> "Электричество (двухтарифный)"
//            else -> "Электричество"
//        }
//    }
//
//    // =====================================================
//    // УНИВЕРСАЛЬНЫЙ МАППИНГ (MeterInfo → UI/Entity)
//    // =====================================================
//
//    /**
//     * Маппинг MeterInfo → MeterUiModel
//     */
//    fun mapToUi(
//        meterInfo: GetMeters.MeterInfo,
//        accountId: String
//    ): MeterUiModel {
//        return MeterUiModel(
//            id = "${accountId}_${meterInfo.id}",
//            accountId = accountId,
//            type = meterInfo.type,
//            serialNumber = meterInfo.serialNumber,
//            lastValue = meterInfo.lastValue,
//            lastSubmissionDate = meterInfo.lastSubmissionDate
//        )
//    }
//
//    /**
//     * Маппинг MeterInfo → MeterEntity
//     */
//    fun mapToEntity(
//        meterInfo: GetMeters.MeterInfo,
//        accountId: String
//    ): MeterEntity {
//        return MeterEntity(
//            id = "${accountId}_${meterInfo.id}",
//            accountId = accountId,
//            apiCounterId = meterInfo.apiCounterId,
//            type = meterInfo.type,
//            serialNumber = meterInfo.serialNumber,
//            lastSubmissionDate = meterInfo.lastSubmissionDate
//        )
//    }
//
//    /**
//     * Маппинг списка
//     */
//    fun mapListToUi(
//        meters: List<GetMeters.MeterInfo>,
//        accountId: String
//    ): List<MeterUiModel> {
//        return meters.map { mapToUi(it, accountId) }
//    }
//
//    fun mapListToEntity(
//        meters: List<GetMeters.MeterInfo>,
//        accountId: String
//    ): List<MeterEntity> {
//        return meters.map { mapToEntity(it, accountId) }
//    }
//}
