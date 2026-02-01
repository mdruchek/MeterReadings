// app/src/main/java/ru/dr/meterreadings/data/mappers/KvcPeriodMapper.kt

package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.data.remote.dto.kvc.KvcTransmissionPeriodDto

/**
 * Маппер для преобразования периода передачи показаний КВЦ
 */
object KvcPeriodMapper {

    /**
     * Преобразует период из API в пару дней (начало-конец)
     *
     * @param transitDays Период из API КВЦ
     * @return Пара: (день начала, день окончания)
     */
    fun mapToTransmissionPeriod(transitDays: KvcTransmissionPeriodDto): Pair<Int, Int> {
        return Pair(
            first = transitDays.first,   // День начала периода (1-31)
            second = transitDays.last    // День окончания периода (1-31)
        )
    }

    /**
     * Извлекает день начала периода
     */
    fun getStartDay(transitDays: KvcTransmissionPeriodDto): Int {
        return transitDays.first
    }

    /**
     * Извлекает день окончания периода
     */
    fun getEndDay(transitDays: KvcTransmissionPeriodDto): Int {
        return transitDays.last
    }
}
