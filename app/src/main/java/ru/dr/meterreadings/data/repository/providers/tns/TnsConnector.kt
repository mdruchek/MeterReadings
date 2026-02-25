package ru.dr.meterreadings.data.repository.providers.tns

//import ru.dr.meterreadings.data.mappers.TnsRegionMapper
//import ru.dr.meterreadings.data.mappers.UniversalAccountMapper
//import ru.dr.meterreadings.data.mappers.UniversalMeterMapper
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.domain.connector.AppAuth
import ru.dr.meterreadings.domain.connector.AuthException
import ru.dr.meterreadings.domain.connector.GetRegions
import ru.dr.meterreadings.domain.connector.ProviderConnector
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.domain.connector.UserAuth
import ru.dr.meterreadings.domain.constants.ProviderIds
import ru.dr.meterreadings.domain.service.AuthService
import ru.dr.meterreadings.models.domain.AuthTokenDomainModel
import ru.dr.meterreadings.utils.safeAuthenticatedCall
import ru.dr.meterreadings.utils.toUserFriendlyMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Коннектор для провайдера ТНС
 *
 * Реализует интерфейс HasRegions для поддержки регионального деления.
 * По мере изучения API будут добавлены другие интерфейсы:
 * - SearchAccount (поиск лицевого счёта)
 * - GetMeters (список счётчиков)
 * - SubmitReadings (отправка показаний)
 */
@Singleton
class TnsConnector @Inject constructor(
    private val repository: TnsRepository,
    private val authService: AuthService,
    private val accountRepository: AccountRepository
) : ProviderConnector,
    AppAuth,
    GetRegions,
    UserAuth,
    GetAccounts
//    GetMeters
{

    override val providerId: Long = ProviderIds.TNS
    override val providerName: String = "ТНС Энерго"

    override suspend fun appAuth(): Result<Boolean> {
        println("🔐 [TnsConnector] Авторизация приложения...")
        return repository.authorizeApp()
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

        return repository.getRegions()
            .map { dtoList ->
                dtoList.map { dto ->
                    GetRegions.RegionInfo(
                        id = dto.id,
                        name = dto.name
                    )
                }
            }
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
            val authResult = repository.authorizeUser(
                login = login,
                password = password,
                regionCode = region
            )

            authResult.onSuccess { authDto ->
                // Сохраняем токен
                val token = AuthTokenDomainModel(
                    providerId = providerId,
                    login = login,
                    accessToken = authDto.accessToken,
                    refreshToken = authDto.refreshToken,
                    accessTokenExpiresAt = parseDateTime(authDto.accessTokenExpires),
                    refreshTokenExpiresAt = parseDateTime(authDto.refreshTokenExpires)
                )

                authService.saveToken(token)
                println("✅ [TnsConnector] Токен сохранён для $login")
            }

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
            println("❌ [TnsConnector] Исключение: ${e.message}")
            e.printStackTrace()

            // ✅ Очищаем сообщение для пользователя
            Result.failure(Exception(e. toUserFriendlyMessage()))
        }
    }

    /**
     * Преобразует строку даты "2026-06-03 09:52:22" в timestamp (миллисекунды)
     */
    private fun parseDateTime(dateString: String): Long {
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            formatter.parse(dateString)?.time ?: Long.MAX_VALUE
        } catch (e: Exception) {
            println("⚠️ [TnsConnector] Не удалось распарсить дату: $dateString")
            Long.MAX_VALUE // ← Если не удалось распарсить — "бесконечный" срок
        }
    }

    /**
     * Получить список лицевых счетов пользователя
     *
     * Для ТНС требуется авторизация, поэтому этот метод
     * загружает ВСЕ аккаунты пользователя (API не поддерживает поиск по номеру).
     *
     * @param accountNumber Номер лицевого счета (не используется для ТНС)
     * @param regionId Код региона — используется ТОЛЬКО для построения URL API
     * @return Result со списком всех аккаунтов пользователя
     */
    override suspend fun getAccounts(
        accountNumber: String,
        regionId: String?,
        login: String?
    ): Result<List<GetAccounts.AccountInfo>> {
        println("🌐 [TnsConnector] getAccounts($accountNumber, regionId=$regionId), login=$login")

        // ✅ Ранний возврат при ошибке
        if (regionId == null) {
            println("❌ [TnsConnector] Ошибка: regionId == null")
            return Result.failure(IllegalArgumentException("Укажите регион"))
        }

        if (login == null) {
            println("❌ [TnsConnector] Ошибка: login == null")
            return Result.failure(IllegalArgumentException("Для Tns необходим login"))
        }

        return safeAuthenticatedCall(authService, providerId, login) { accessToken ->
            repository.getAccounts(accessToken, regionId)
        }.map { accountsList ->
            println("✅ [TnsConnector] Получено аккаунтов: ${accountsList.size}")

            accountsList.map { account ->  // ✅ account — один TnsAccountDto
                GetAccounts.AccountInfo(
                    number = account.number,      // ✅ Используем свойства account
                    address = account.address,
                    login = login,
                    submissionStartDay = null,
                    submissionEndDay = null,
                    additionalInfo = null
                )
            }
        }.onFailure { error ->
            println("❌ [TnsConnector] Ошибка: ${error.message}")

            // ✅ Очищаем токен при ошибке авторизации
            if (error is AuthException) {
                println("🗑️ [TnsConnector] Ошибка авторизации - очищаем токен")
                authService.clearToken(providerId, login)
            }
        }
    }


    /**
     * Получить список счётчиков для лицевого счёта
     */
//    override suspend fun getMeters(
//        accountNumber: String,
//        regionId: String?
//    ): Result<GetMeters.GetMetersResult> {
//        val region = requireNotNull(regionId) { "Укажите регион" }
//
//        return try {
//            // ✅ Теперь account будет AccountDomainModel
//            val account = accountRepository.findByAccountNumber(accountNumber)
//                ?: return Result.failure(Exception("Аккаунт не найден"))
//
//            val userLogin = account.login
//                ?: return Result.failure(Exception("У аккаунта нет логина"))
//
//            safeAuthenticatedCall(authService, providerId, userLogin) { accessToken ->
//                val result = repository.getCounters(accountNumber, accessToken, region)
//                result.getOrThrow().let { dtoList ->
//                    val meters = UniversalMeterMapper.fromTnsDtoList(dtoList)
//                    GetMeters.GetMetersResult(meters = meters, cacheData = null)
//                }
//            }
//        } catch (e: Exception) {
//            println("❌ [TnsConnector] Ошибка: ${e.message}")
//            Result.failure(Exception(e.toUserFriendlyMessage()))
//        }
//    }

}