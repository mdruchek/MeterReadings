// app/src/main/java/ru/dr/meterreadings/data/mappers/TnsRegionMapper.kt
package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.data.remote.dto.tns.TnsRegionDto
import ru.dr.meterreadings.domain.connector.GetRegions

/**
 * Маппер для преобразования регионов ТНС
 */
object TnsRegionMapper {

    /**
     * Преобразует TnsRegionDto → HasRegions.RegionInfo
     */
    fun mapToRegionInfo(dto: TnsRegionDto): GetRegions.RegionInfo {
        return UniversalRegionMapper.mapToRegionInfo(
            id = dto.code,      // "voronezh", "kuban"
            name = dto.name     // "Воронежская область"
        )
    }

    /**
     * Преобразует список регионов ТНС
     */
    fun mapListToRegionInfo(dtoList: List<TnsRegionDto>): List<GetRegions.RegionInfo> {
        return dtoList.map { mapToRegionInfo(it) }
    }
}
