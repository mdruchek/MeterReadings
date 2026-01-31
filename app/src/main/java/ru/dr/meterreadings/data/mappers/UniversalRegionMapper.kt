// app/src/main/java/ru/dr/meterreadings/data/mappers/UniversalRegionMapper.kt
package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.domain.connector.GetRegions

/**
 * Универсальный маппер для регионов от любого провайдера
 *
 * Преобразует DTO регионов в стандартный формат HasRegions.RegionInfo
 */
object UniversalRegionMapper {

    /**
     * Маппинг DTO региона → HasRegions.RegionInfo
     *
     * @param id ID региона (может быть code, number и т.д.)
     * @param name Название региона
     * @return Унифицированный объект региона
     */
    fun mapToRegionInfo(
        id: String,
        name: String
    ): GetRegions.RegionInfo {
        return GetRegions.RegionInfo(
            id = id,
            name = name
        )
    }

    /**
     * Маппинг списка регионов
     *
     * @param regions Список пар (id, name)
     * @return Список унифицированных регионов
     */
    fun mapListToRegionInfo(
        regions: List<Pair<String, String>>
    ): List<GetRegions.RegionInfo> {
        return regions.map { (id, name) ->
            mapToRegionInfo(id, name)
        }
    }
}
