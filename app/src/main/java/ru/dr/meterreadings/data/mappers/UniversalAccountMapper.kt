package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.data.remote.dto.tns.TnsAccountDto
import ru.dr.meterreadings.domain.connector.GetAccounts

/**
 * Универсальный маппер для лицевых счетов всех провайдеров
 *
 * Преобразует DTO-модели провайдеров в стандартный AccountInfo.
 */
object UniversalAccountMapper {

    /**
     * Маппинг TNS аккаунта (один)
     *
     * @param dto TnsAccountDto из API
     * @param regionId Код региона (например, "rostov")
     * @param login Email пользователя (для сохранения в БД)
     */
    fun fromTnsDto(
        dto: TnsAccountDto,
        regionId: String,
        login: String,
        additionalInfo: String?
    ): GetAccounts.AccountInfo {
        return GetAccounts.AccountInfo(
            accountNumber = dto.number,
            address = dto.address,
            regionId = regionId,
            login = login,
            submissionStartDay = null,
            submissionEndDay = null,
            additionalInfo = additionalInfo
        )
    }

    /**
     * Маппинг списка TNS аккаунтов
     *
     * @param dtoList Список TnsAccountDto из API
     * @param regionId Код региона (например, "rostov")
     * @param login Email пользователя (для сохранения в БД)
     */
    fun fromTnsDtoList(
        dtoList: List<TnsAccountDto>,
        regionId: String,
        login: String, // ✅ ДОБАВЛЕН ПАРАМЕТР
        additionalInfo: String?
    ): List<GetAccounts.AccountInfo> {
        return dtoList.map { fromTnsDto(it, regionId, login, additionalInfo) } // ✅ ПЕРЕДАЁМ LOGIN
    }

    // TODO: Добавить маппинг для других провайдеров (KVC, etc.)
    // fun fromKvcDto(dto: KvcAccountDto): GetAccounts.AccountInfo { ... }
}
