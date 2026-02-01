package ru.dr.meterreadings.data.repository.providers.tns

import ru.dr.meterreadings.data.mappers.TnsRegionMapper
import ru.dr.meterreadings.domain.connector.AppAuth
import ru.dr.meterreadings.domain.connector.GetRegions
import ru.dr.meterreadings.domain.connector.ProviderConnector
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.domain.connector.UserAuth
import ru.dr.meterreadings.domain.constants.ProviderIds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Коннектор для провайдера ТНС
 *
 * Реализует интерфейс HasRegions для поддержки регионального деления.
 * По мере изучения API будут добавлены другие интерфейсы:
 * - SearchAccount (поиск лицевого счёта)
 * - LoadMeters (загрузка счётчиков)
 * - SubmitReadings (отправка показаний)
 */
@Singleton
class TnsConnector @Inject constructor(
    private val tnsRepository: TnsRepository
) : ProviderConnector,
    AppAuth,
    GetRegions,
    UserAuth{

    override val providerId: Long = ProviderIds.TNS
    override val providerName: String = "ТНС Энерго"

    override suspend fun appAuth(): Result<Boolean> {
        println("🔐 [TnsConnector] Авторизация приложения...")
        return tnsRepository.authorizeApp()
    }

    /**
     * Получить список регионов ТНС
     *
     * Преобразует TnsRegionDto из repository в HasRegions.RegionInfo
     * для унификации с другими провайдерами.
     *
     * @return Result со списком регионов в стандартном формате
     */
    override suspend fun getRegions(): Result<List<GetRegions.RegionInfo>> {
        println("🌐 [TnsConnector] Запрос регионов...")

        val result = tnsRepository.getRegions()

        return result.fold(
            onSuccess = { regions ->
                println("✅ [TnsConnector] Получено регионов: ${regions.size}")

                // Преобразуем TnsRegionDto → HasRegions.RegionInfo
                val regionInfoList = TnsRegionMapper.mapListToRegionInfo(regions)

                Result.success(regionInfoList)
            },
            onFailure = { error ->
                println("❌ [TnsConnector] Ошибка: ${error.message}")
                Result.failure(error)
            }
        )
    }

    /**
     * Авторизация пользователя ТНС
     *
     * @param login Email пользователя
     * @param password Пароль
     * @param regionId Код региона (например, "nn")
     * @return Result с токенами авторизации
     */
    override suspend fun userAuth(
        login: String,
        password: String,
        regionId: String?
    ): Result<UserAuth.UserAuthData> {
        println("🔐 [TnsConnector] Авторизация пользователя: $login")

        return try {
            val region = requireNotNull(regionId) {
                "Для ТНС необходимо указать регион"
            }

            // Выполняем авторизацию
            val authResult = tnsRepository.authorizeUser(
                login = login,
                password = password,
                regionCode = region
            )

            authResult.map { authDto ->
                // Маппинг DTO → Domain Model
                UserAuth.UserAuthData(
                    authSuccess = true,
                    accessToken = authDto.accessToken,
                    refreshToken = authDto.refreshToken,
                    accessTokenExpires = authDto.accessTokenExpires,
                    refreshTokenExpires = authDto.refreshTokenExpires
                )
            }
        } catch (e: Exception) {
            println("❌ [TnsConnector] Ошибка авторизации: ${e.message}")
            Result.failure(e)
        }
    }
}