// app/src/main/java/ru/dr/meterreadings/data/mappers/UniversalMeterMapper.kt
package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.data.local.entities.MeterEntity
import ru.dr.meterreadings.domain.connector.LoadMeters
import ru.dr.meterreadings.models.ui.MeterUiModel

/**
 * Универсальный маппер для счётчиков от любого провайдера
 */
object UniversalMeterMapper {

    /**
     * Маппинг MeterInfo → MeterUiModel
     */
    fun mapToUi(
        meterInfo: LoadMeters.MeterInfo,
        accountId: String
    ): MeterUiModel {
        return MeterUiModel(
            id = "${accountId}_${meterInfo.id}",
            accountId = accountId,
            type = meterInfo.type,
            serialNumber = meterInfo.serialNumber,
            lastValue = meterInfo.lastValue,
            lastSubmissionDate = meterInfo.lastSubmissionDate
        )
    }

    /**
     * Маппинг MeterInfo → MeterEntity
     */
    fun mapToEntity(
        meterInfo: LoadMeters.MeterInfo,
        accountId: String
    ): MeterEntity {
        return MeterEntity(
            id = "${accountId}_${meterInfo.id}",
            accountId = accountId,
            apiCounterId = meterInfo.apiCounterId,
            type = meterInfo.type,
            serialNumber = meterInfo.serialNumber,
            lastSubmissionDate = meterInfo.lastSubmissionDate
        )
    }

    /**
     * Маппинг списка
     */
    fun mapListToUi(
        meters: List<LoadMeters.MeterInfo>,
        accountId: String
    ): List<MeterUiModel> {
        return meters.map { mapToUi(it, accountId) }
    }

    fun mapListToEntity(
        meters: List<LoadMeters.MeterInfo>,
        accountId: String
    ): List<MeterEntity> {
        return meters.map { mapToEntity(it, accountId) }
    }
}
