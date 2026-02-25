package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.domain.connector.GetMeters
import ru.dr.meterreadings.domain.connector.SubmitReadings
import ru.dr.meterreadings.domain.connector.ValidateReading
import ru.dr.meterreadings.domain.connector.GetMeterHistory
import ru.dr.meterreadings.domain.connector.UserAuth
import ru.dr.meterreadings.domain.connector.AuthException
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.domain.exceptions.AccountNotFoundException
import ru.dr.meterreadings.domain.service.CaptchaService
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.domain.MeterDomainModel
import ru.dr.meterreadings.models.domain.toMeterDomainModel
import ru.dr.meterreadings.models.ui.MeterUiModel
import ru.dr.meterreadings.models.ui.AuthError
import ru.dr.meterreadings.ui.components.CaptchaSession
import ru.dr.meterreadings.utils.toUserFriendlyMessage

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val providerConnectorFactory: ProviderConnectorFactory,
    private val captchaService: CaptchaService,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    // ============================================
    // Получаем profileId из навигации
    // ============================================
    private val profileId: String = checkNotNull(savedStateHandle["profileId"])

    // ============================================
    // STATE
    // ============================================

    /** Данные профиля (имя) */
    private val _profile = MutableStateFlow<ProfileDomainModel?>(null)
    val profile: StateFlow<ProfileDomainModel?> = _profile.asStateFlow()

    /** Список аккаунтов (ЛС) профиля */
    private val _accounts = MutableStateFlow<List<AccountDomainModel>>(emptyList())
    val accounts: StateFlow<List<AccountDomainModel>> = _accounts.asStateFlow()

    /** Карта загруженных счётчиков: accountId → список счётчиков */
    private val _accountMeters = MutableStateFlow<Map<String, List<MeterDomainModel>>>(emptyMap())
    val accountMeters: StateFlow<Map<String, List<MeterDomainModel>>> = _accountMeters.asStateFlow()

    /** Множество ID аккаунтов, для которых сейчас загружаются счётчики */
    private val _loadingAccounts = MutableStateFlow<Set<String>>(emptySet())
    val loadingAccounts: StateFlow<Set<String>> = _loadingAccounts.asStateFlow()

    /** Карта ошибок загрузки счётчиков: accountId → сообщение ошибки */
    private val _accountErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val accountErrors: StateFlow<Map<String, String>> = _accountErrors.asStateFlow()

    /** Карта отправляющихся счётчиков: accountId → Set<meterId> */
    private val _submittingMeters = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val submittingMeters: StateFlow<Map<String, Set<String>>> = _submittingMeters.asStateFlow()

    /**
     * История показаний, сгруппированная по аккаунтам и счётчикам
     * Структура: Map<accountId, Map<meterId, List<HistoryEntry>>>
     */
    private val _counterHistories = MutableStateFlow<Map<String, Map<String, List<GetMeterHistory.MeterHistory>>>>(emptyMap())
    val counterHistories: StateFlow<Map<String, Map<String, List<GetMeterHistory.MeterHistory>>>> = _counterHistories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _authError = MutableStateFlow<AuthError?>(null)
    val authError: StateFlow<AuthError?> = _authError.asStateFlow()

    /**
     * Очистить ошибку авторизации
     */
    fun dismissAuthError() {
        _authError.value = null
    }

    /**
     * Установить ошибку (для UI)
     */
    fun setError(message: String) {
        _error.value = message
    }

    // ==================== STATE - КАПЧА ====================

    /** Показывать ли UI капчи */
    private val _showCaptcha = MutableStateFlow(false)
    val showCaptcha: StateFlow<Boolean> = _showCaptcha.asStateFlow()

    // ============================================
    // INIT
    // ============================================
    init {
        println("🔵 [ProfileDetailViewModel] Создание для profileId: $profileId")
        getProfile()
        observeAccounts()
    }

    // ============================================
    // PUBLIC METHODS
    // ============================================

    /**
     * Очистить ошибку
     */
    fun clearError() {
        _error.value = null
    }

    private var pendingAccountId: String? = null

    /**
     * Загрузить счётчики для конкретного аккаунта
     *
     * При ошибке сохраняет её в [accountErrors].
     * Если ошибка авторизации - сохраняет в [authError].
     */
    fun loadMetersForAccount(accountId: String) {
        viewModelScope.launch {
            println("🔍 [ProfileDetailViewModel] Загрузка счётчиков для аккаунта $accountId")

            _accountErrors.value = _accountErrors.value - accountId
            _loadingAccounts.value = _loadingAccounts.value + accountId

            try {
                val account = _accounts.value.firstOrNull { it.id == accountId }
                    ?: throw IllegalArgumentException("Аккаунт не найден")

                val connector = providerConnectorFactory.getConnector(account.providerId)
                if (connector !is GetMeters) {
                    throw IllegalStateException("Провайдер не поддерживает загрузку счётчиков")
                }

                val result = connector.getMeters(
                    accountNumber = account.number,
                    regionId = account.regionId,
                    apiAccountId = account.uuid
                )

                result.onSuccess { metersResult ->
                    println("✅ [ProfileDetailViewModel] Счётчики загружены: ${metersResult.meters.size}")
                    _accountMeters.value = _accountMeters.value +
                            (accountId to metersResult.meters.map { it.toMeterDomainModel(accountId, account.providerId) })
                }.onFailure { error ->
                    if (error is AccountNotFoundException && connector is GetAccounts) {
                        println("🔄 [ProfileDetailViewModel] UUID устарел, обновляем аккаунт...")
                        pendingAccountId = accountId
                        _showCaptcha.value = true
                    } else {
                        _accountErrors.value = _accountErrors.value + (accountId to error.toUserFriendlyMessage())
                    }
                }
            } catch (e: Exception) {
                _accountErrors.value = _accountErrors.value + (accountId to e.toUserFriendlyMessage())
            } finally {
                _loadingAccounts.value = _loadingAccounts.value - accountId
            }
        }
    }


    /**
     * Отправить показание счётчика
     *
     * @param meter UI модель счётчика
     * @param newValue Новое показание
     */
    fun submitReading(meter: MeterUiModel, newValue: Int) {
        viewModelScope.launch {
            // ✅ Добавляем в список "отправляется"
            val accountSubmitting = _submittingMeters.value[meter.accountId] ?: emptySet()
            _submittingMeters.value = _submittingMeters.value + (meter.accountId to (accountSubmitting + meter.id))

            try {
                println("📤 [ProfileDetailViewModel] Отправка показания: ${meter.type} = $newValue")

                // ШАГ 1: Находим аккаунт
                val account = _accounts.value.firstOrNull { it.id == meter.accountId }
                    ?: throw Exception("Аккаунт не найден")

                // ШАГ 2: Получаем коннектор
                val connector = providerConnectorFactory.getConnector(account.providerId)
                if (connector !is SubmitReadings) {
                    throw Exception("Провайдер не поддерживает отправку показаний")
                }

                // ШАГ 4: Загружаем историю (для валидации)
                println("🔍 [ProfileDetailViewModel] Загружаем историю для валидации...")
                val historyLoadResult = loadCounterHistory(
                    accountId = meter.accountId,
                    meterId = meter.id
                )

                if (historyLoadResult.isFailure) {
                    println("⚠️ [ProfileDetailViewModel] Не удалось загрузить историю")
                }

                // ШАГ 5: Валидация через ValidateReading
                if (connector is ValidateReading) {
                    println("✅ [ProfileDetailViewModel] Провайдер поддерживает валидацию")

                    val minValueResult = connector.getMinimumAllowedValue(
                        counterId = meter.id,
                        accountNumber = account.number,
                        regionId = account.regionId
                    )

                    minValueResult.onSuccess { minValue ->
                        if (minValue != null) {
                            println("📊 [ProfileDetailViewModel] Минимальное значение: $minValue")
                            if (newValue < minValue) {
                                throw Exception(
                                    "Показание не может быть меньше $minValue\n" +
                                            "(минимально допустимое значение)"
                                )
                            }
                            println("✅ [ProfileDetailViewModel] Валидация пройдена: $newValue >= $minValue")
                        } else {
                            println("⚠️ [ProfileDetailViewModel] Минимум = null, валидации не будет")
                        }
                    }.onFailure { error ->
                        println("⚠️ [ProfileDetailViewModel] Ошибка валидации: ${error.message}")
                    }
                } else {
                    println("⚠️ [ProfileDetailViewModel] Провайдер не поддерживает ValidateReading")
                }

                // ШАГ 6: Отправляем показание
                println("📤 [ProfileDetailViewModel] Отправка через ${connector.javaClass.simpleName}")

                val result = connector.submitReading(
                    counterId = meter.id,
                    accountNumber = account.number,
                    value = newValue.toString(),
                    valueNight = null,
                    regionId = account.regionId
                )

                if (result.isFailure) {
                    val exception = result.exceptionOrNull() ?: Exception("Не удалось передать показание")

                    // ✅ ДОБАВЛЕНО: обработка CaptchaRequiredException
                    if (exception is CaptchaRequiredException) {
                        println("🔐 [ProfileDetailViewModel] Требуется капча для submitReading")

                        // Показываем капчу
                        _showCaptcha.value = true

                        return@launch // Не бросаем исключение, показываем капчу
                    }

                    throw exception
                }

                println("✅ [ProfileDetailViewModel] Показание успешно передано")

                // ШАГ 7: Сбрасываем историю для перезагрузки
                println("🗑️ [ProfileDetailViewModel] Очищаем историю для ${meter.id}")
                val accountHistories = _counterHistories.value[meter.accountId]?.toMutableMap() ?: mutableMapOf()
                accountHistories.remove(meter.id)
                _counterHistories.value = _counterHistories.value + (meter.accountId to accountHistories)

                // ШАГ 8: Перезагружаем счётчики аккаунта
                println("🔄 [ProfileDetailViewModel] Перезагружаем счётчики...")
                _accountMeters.value = _accountMeters.value - account.id
                loadMetersForAccount(account.id)

                // ШАГ 9: Перезагружаем историю
                println("📊 [ProfileDetailViewModel] Перезагружаем историю для ${meter.id}...")
                loadCounterHistory(
                    accountId = meter.accountId,
                    meterId = meter.id
                )

            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка передачи: ${e.message}")
                e.printStackTrace()

                _error.value = when {
                    e.message?.contains("Период") == true -> e.message
                    e.message?.contains("Передача доступна") == true -> e.message
                    e.message?.contains("не может быть меньше") == true -> e.message
                    else -> "Не удалось передать показание: ${e.message}"
                }
            } finally {
                // ✅ Убираем из списка "отправляется"
                val accountSubmitting = _submittingMeters.value[meter.accountId]?.minus(meter.id) ?: emptySet()
                if (accountSubmitting.isEmpty()) {
                    _submittingMeters.value = _submittingMeters.value - meter.accountId
                } else {
                    _submittingMeters.value = _submittingMeters.value + (meter.accountId to accountSubmitting)
                }
            }
        }
    }

    /**
     * Загрузить историю показаний счётчика
     *
     * @param accountId ID аккаунта
     * @param meterId API ID счётчика
     * @return Result с историей или ошибкой
     */
    suspend fun loadCounterHistory(
        accountId: String,
        meterId: String
    ): Result<List<GetMeterHistory.MeterHistory>> {
        return try {
            println("🔍 [ProfileDetailViewModel] Запрос истории для счётчика $meterId в аккаунте $accountId")

            // ✅ Проверяем кеш (двойная вложенность)
            if (_counterHistories.value[accountId]?.containsKey(meterId) == true) {
                val cachedHistory = _counterHistories.value[accountId]!![meterId]!!
                println("✅ [ProfileDetailViewModel] История уже загружена (${cachedHistory.size} записей)")
                return Result.success(cachedHistory)
            }

            // ✅ Находим аккаунт
            val account = _accounts.value.firstOrNull { it.id == accountId }
                ?: return Result.failure(Exception("Аккаунт не найден"))

            // ✅ Находим счётчик в конкретном аккаунте
            val meter = _accountMeters.value[accountId]?.firstOrNull { it.id == meterId }
                ?: return Result.failure(Exception("Счётчик не найден"))

            // Получаем коннектор
            val connector = providerConnectorFactory.getConnector(account.providerId)
            if (connector !is GetMeterHistory) {
                println("⚠️ [ProfileDetailViewModel] Провайдер ${account.providerId} не поддерживает историю")
                return Result.failure(Exception("Провайдер не поддерживает загрузку истории"))
            }

            // Загружаем историю
            val historyResult = connector.getMeterHistory(
                counterId = meterId,
                accountNumber = account.number,
                regionId = account.regionId
            )

            historyResult.fold(
                onSuccess = { history ->
                    println("✅ [ProfileDetailViewModel] Загружено ${history.size} записей истории")

                    // ✅ Сохраняем с двойной вложенностью
                    val accountHistories = _counterHistories.value[accountId]?.toMutableMap() ?: mutableMapOf()
                    accountHistories[meterId] = history
                    _counterHistories.value = _counterHistories.value + (accountId to accountHistories)

                    Result.success(history)
                },
                onFailure = { error ->
                    // ✅ ДОБАВЛЕНО: обработка капчи
                    if (error is CaptchaRequiredException) {
                        println("🔐 [ProfileDetailViewModel] Требуется капча для getMeterHistory")
                        _showCaptcha.value = true
                    }

                    println("❌ [ProfileDetailViewModel] Ошибка загрузки истории: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ [ProfileDetailViewModel] Исключение: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }


    /**
     * Получить минимально допустимое значение для счётчика (для UI валидации)
     *
     * Вызывается из UI для динамической валидации поля ввода.
     * Работает ТОЛЬКО если история уже загружена в _counterHistories.
     *
     * @param meterId UI ID счётчика
     * @return минимальное значение или null
     */
    suspend fun getMinimumValueForMeter(
        accountId: String,
        meterId: String
    ): Int? {
        return try {
            println("🔍 [ProfileDetailViewModel] Запрос минимума для UI: $meterId")

            // ✅ Проверяем, есть ли история (двойная вложенность)
            if (_counterHistories.value[accountId]?.containsKey(meterId) != true) {
                println("⚠️ [ProfileDetailViewModel] История для $meterId не загружена")
                return null
            }

            println("✅ [ProfileDetailViewModel] История найдена, получаем минимум")

            // ✅ Находим аккаунт
            val account = _accounts.value.firstOrNull { it.id == accountId }
            if (account == null) {
                println("❌ [ProfileDetailViewModel] Аккаунт $accountId не найден")
                return null
            }

            // ✅ Находим счётчик в конкретном аккаунте
            val meter = _accountMeters.value[accountId]?.firstOrNull { it.id == meterId }
            if (meter == null) {
                println("❌ [ProfileDetailViewModel] Счётчик $meterId не найден")
                return null
            }

            // Получаем коннектор
            val connector = providerConnectorFactory.getConnector(account.providerId)
            if (connector !is ValidateReading) {
                println("⚠️ [ProfileDetailViewModel] Провайдер ${account.providerId} не поддерживает ValidateReading")
                return null
            }

            // Получаем минимум
            val minValueResult = connector.getMinimumAllowedValue(
                counterId = meterId,
                accountNumber = account.number,
                regionId = account.regionId
            )

            val minValue = minValueResult.getOrNull()
            println("📊 [ProfileDetailViewModel] Минимум для UI: ${minValue ?: "не определён"}")
            minValue
        } catch (e: Exception) {
            println("❌ [ProfileDetailViewModel] Ошибка получения минимума: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Повторная авторизация после истечения токена
     */
    fun reauthenticate(
        login: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ Берём любой аккаунт для получения providerId и regionId
                val account = _accounts.value.firstOrNull()
                if (account == null) {
                    onFailure("Нет аккаунтов для авторизации")
                    return@launch
                }

                val connector = providerConnectorFactory.getConnector(account.providerId)

                if (connector !is UserAuth) {
                    onFailure("Провайдер не поддерживает авторизацию")
                    return@launch
                }

                println("🔐 [ProfileDetailVM] Повторная авторизация: $login")

                val result = connector.userAuth(
                    login = login,
                    password = password,
                    regionId = account.regionId?.toString()
                )

                result.fold(
                    onSuccess = { authData ->
                        if (authData.authSuccess) {
                            println("✅ [ProfileDetailVM] Повторная авторизация успешна")
                            onSuccess()
                        } else {
                            onFailure("Неверный логин или пароль")
                        }
                    },
                    onFailure = { error ->
                        println("❌ [ProfileDetailVM] Ошибка: ${error.message}")
                        onFailure(error.message ?: "Не удалось авторизоваться")
                    }
                )
            } catch (e: Exception) {
                onFailure(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Удалить аккаунт
     */
    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                accountRepository.deleteAccount(accountId)
                println("✅ [ProfileDetailViewModel] Аккаунт удалён")

                // ✅ Очищаем все связанные данные
                _accountMeters.value = _accountMeters.value - accountId
                _counterHistories.value = _counterHistories.value - accountId
                _submittingMeters.value = _submittingMeters.value - accountId
                _accountErrors.value = _accountErrors.value - accountId
            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка удаления: ${e.message}")
                _error.value = "Не удалось удалить аккаунт"
            }
        }
    }

    // ============================================
    // PRIVATE METHODS
    // ============================================

    private fun getProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            profileRepository.getProfileById(profileId)
                .catch { error ->
                    println("❌ [ProfileDetailViewModel] Ошибка загрузки профиля: ${error.message}")
                    _error.value = "Не удалось загрузить профиль"
                    _isLoading.value = false
                }
                .collectLatest { profile ->
                    _profile.value = profile
                    println("✅ [ProfileDetailViewModel] Профиль загружен: ${profile?.name}")
                    // Выключаем индикатор загрузки после первого значения
                    if (_isLoading.value) {
                        _isLoading.value = false
                    }
                }
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            try {
                accountRepository.getAccountsByProfileId(profileId).collect { accounts ->
                    println("🔄 [ProfileDetailViewModel] Аккаунты обновились: ${accounts.size} шт.")
                    _accounts.value = accounts
                    if (accounts.isEmpty()) {
                        println("⚠️ [ProfileDetailViewModel] Аккаунтов нет")
                    }
                }
            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка подписки на аккаунты: ${e.message}")
                _error.value = "Не удалось загрузить аккаунты"
            }
        }
    }

    // Метод для очистки ошибки конкретного аккаунта
    fun clearAccountError(accountId: String) {
        _accountErrors.value = _accountErrors.value - accountId
    }

    // ==================== КАПЧА ====================

    /**
     * Вызывается после успешного прохождения капчи
     */
    fun onCaptchaCompleted(session: CaptchaSession) {
        viewModelScope.launch {
            println("✅ [ProfileDetailViewModel] Капча пройдена: ${session.token.take(20)}...")
            _showCaptcha.value = false

            val accountId = pendingAccountId ?: return@launch
            pendingAccountId = null

            val account = _accounts.value.find { it.id == accountId } ?: return@launch
            val connector = providerConnectorFactory.getConnector(account.providerId)
            if (connector !is GetAccounts) return@launch

            // 1. Сохраняем токен капчи
            captchaService.saveCaptchaSession(account.providerId, account.number, session)

            // 2. Получаем свежие данные аккаунта
            val result = connector.getAccounts(
                accountNumber = account.number,
                regionId = account.regionId,
                login = account.login
            )

            result.onSuccess { accounts ->
                val freshInfo = accounts.firstOrNull() ?: return@onSuccess

                // 3. Обновляем аккаунт в БД (новый UUID и т.п.)
                val updatedAccount = account.copy(
                    uuid = freshInfo.uuid,
                    address = freshInfo.address
                )
                accountRepository.updateAccount(updatedAccount)

                // 4. Обновляем период передачи провайдера
                if (freshInfo.submissionStartDay != null && freshInfo.submissionEndDay != null) {
                    providerRepository.updateProviderTransmissionPeriod(
                        providerId = account.providerId,
                        periodStartDay = freshInfo.submissionStartDay,
                        periodEndDay = freshInfo.submissionEndDay
                    )
                }

                // 5. Повторно грузим счётчики
                println("🔄 [ProfileDetailViewModel] UUID обновлён, повторная загрузка счётчиков...")
                loadMetersForAccount(accountId)
            }.onFailure { error ->
                println("❌ [ProfileDetailViewModel] Ошибка обновления аккаунта: ${error.message}")
                _accountErrors.value = _accountErrors.value + (accountId to error.toUserFriendlyMessage())
            }
        }
    }

    /**
     * Отмена капчи пользователем
     */
    fun dismissCaptcha() {
        _showCaptcha.value = false
        pendingAccountId = null
        println("❌ [ProfileDetailViewModel] Капча отменена пользователем")
    }


    // ============================================
    // LIFECYCLE
    // ============================================

    override fun onCleared() {
        super.onCleared()
        println("🧹 [ProfileDetailViewModel] ViewModel очищается → счётчики обнуляются")
    }
}
