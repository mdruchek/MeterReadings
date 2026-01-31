package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.domain.connector.GetMeters
import ru.dr.meterreadings.domain.connector.GetTransmissionPeriod
import ru.dr.meterreadings.domain.connector.SubmitReadings
import ru.dr.meterreadings.domain.connector.ValidateReading
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.ui.MeterUiModel  // ✅ Импорт твоей модели
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.plus

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val providerConnectorFactory: ProviderConnectorFactory
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

    /** Счётчики, сгруппированные по аккаунтам (Map вместо List) */
    private val _accountMeters = MutableStateFlow<Map<String, List<MeterUiModel>>>(emptyMap())
    val accountMeters: StateFlow<Map<String, List<MeterUiModel>>> = _accountMeters.asStateFlow()

    /** Набор ID аккаунтов, для которых идёт загрузка счётчиков */
    private val _loadingAccounts = MutableStateFlow<Set<String>>(emptySet())
    val loadingAccounts: StateFlow<Set<String>> = _loadingAccounts.asStateFlow()

    /** Общий флаг загрузки (профиль, аккаунты) */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Текст ошибки */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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

    /**
     * Загрузить счётчики для конкретного аккаунта
     *
     * @param accountId ID аккаунта
     */
    fun loadMetersForAccount(accountId: String) {
        // Если уже загружаем или уже загружены — пропускаем
        if (_loadingAccounts.value.contains(accountId)) {
            println("⏭️ [ProfileDetailViewModel] Счётчики для $accountId уже загружаются")
            return
        }

        if (_accountMeters.value.containsKey(accountId)) {
            println("⏭️ [ProfileDetailViewModel] Счётчики для $accountId уже загружены")
            return
        }

        viewModelScope.launch {
            println("🔍 [ProfileDetailViewModel] Загрузка счётчиков для аккаунта $accountId")

            // Добавляем в список загружаемых
            _loadingAccounts.value = _loadingAccounts.value + accountId

            val account = _accounts.value.find { it.id == accountId }
            if (account == null) {
                println("❌ [ProfileDetailViewModel] Аккаунт $accountId не найден")
                _loadingAccounts.value = _loadingAccounts.value - accountId
                return@launch
            }

            val result = getMetersForAccount(account)

            result.onSuccess { metersResult ->
                val meters = metersResult.meters.map { meter ->
                    MeterUiModel(
                        id = "${account.id}_${meter.id}",
                        accountId = account.id,
                        type = meter.type,
                        serialNumber = meter.serialNumber,
                        lastValue = meter.lastValue,
                        lastSubmissionDate = meter.lastSubmissionDate,
                        lastMonthConsumption = null  // ✅ Добавлено из твоей модели
                    )
                }

                // Обновляем Map
                _accountMeters.value = _accountMeters.value + (accountId to meters)
                println("✅ [ProfileDetailViewModel] Загружено ${meters.size} счётчиков для $accountId")

            }.onFailure { error ->
                println("❌ [ProfileDetailViewModel] Ошибка загрузки счётчиков для $accountId: ${error.message}")
                _error.value = "Не удалось загрузить счётчики для аккаунта"
            }

            // Убираем из списка загружаемых
            _loadingAccounts.value = _loadingAccounts.value - accountId
        }
    }

    // ============================================
    // PRIVATE METHODS
    // ============================================

    /**
     * Загружает данные профиля (имя)
     */
    private fun getProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                profileRepository.getProfileById(profileId).collect { profile ->
                    _profile.value = profile
                    println("✅ [ProfileDetailViewModel] Профиль загружен: ${profile?.name}")
                }
            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка загрузки профиля: ${e.message}")
                _error.value = "Не удалось загрузить профиль"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Загружает счётчики для конкретного аккаунта через API
     *
     * @param account Аккаунт, для которого загружаем счётчики
     * @return Result со списком счётчиков или ошибкой
     */
    private suspend fun getMetersForAccount(
        account: AccountDomainModel
    ): Result<GetMeters.GetMetersResult> {
        return try {
            println("🔍 [ProfileDetailViewModel] Загрузка счётчиков для ЛС ${account.accountNumber}")

            val connector = providerConnectorFactory.getConnector(account.providerId)

            if (connector !is GetMeters) {
                val errorMsg = "Провайдер ${account.providerId} не поддерживает загрузку счётчиков"
                println("⚠️ [ProfileDetailViewModel] $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            val result = connector.getMeters(
                accountNumber = account.accountNumber,
                regionId = account.regionId?.toString()
            )

            result.onSuccess { metersResult ->
                println("✅ [ProfileDetailViewModel] Загружено ${metersResult.meters.size} счётчиков для ЛС ${account.accountNumber}")
            }.onFailure { error ->
                println("❌ [ProfileDetailViewModel] Ошибка загрузки счётчиков для ЛС ${account.accountNumber}: ${error.message}")
            }

            result

        } catch (e: Exception) {
            println("❌ [ProfileDetailViewModel] Исключение при загрузке счётчиков: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Подписывается на изменения аккаунтов в БД
     *
     * Flow автоматически обновляется при добавлении/удалении аккаунтов
     */
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

    /**
     * Отправляет новое показание по конкретному счётчику
     *
     * Процесс:
     * 1. Загружаем историю показаний (для валидации)
     * 2. Валидируем введённое значение через ValidateReading
     * 3. Отправляем показание через SubmitReadings
     * 4. Перезагружаем счётчики для обновления UI
     *
     * @param meter UI-модель счётчика
     * @param newValue Новое показание для отправки
     */
    fun submitReading(meter: MeterUiModel, newValue: Int) {
        viewModelScope.launch {
            // Добавляем счётчик в список "отправляется"
            _submittingMeters.value = _submittingMeters.value + meter.id
            try {
                println("📤 [ProfileDetailViewModel] Отправляем показание: ${meter.type} = $newValue")

                // ШАГ 1: Находим аккаунт
                val account = _accounts.value.firstOrNull { it.id == meter.accountId }
                    ?: throw Exception("Аккаунт не найден")

                // ШАГ 2: Получаем коннектор провайдера
                val connector = providerConnectorFactory.getConnector(account.providerId)
                if (connector !is SubmitReadings) {
                    throw Exception("Провайдер не поддерживает отправку показаний")
                }

                // ШАГ 3: Берём кеш и API ID из памяти
                val cacheData = _providerCache.value[account.id]?.rawData
                val apiCounterId = _providerCache.value[account.id]?.meterApiIds?.get(meter.id)
                    ?: throw Exception("Данные счётчика не загружены. Обновите страницу.")

                println("📋 [ProfileDetailViewModel] API ID счётчика: $apiCounterId")
                println("📦 [ProfileDetailViewModel] Кеш: ${if (cacheData != null) "ЕСТЬ" else "НЕТ"}")

                // ШАГ 4: Загружаем историю показаний (для валидации)
                println("🔍 [ProfileDetailViewModel] Загружаем историю для валидации...")
                val historyLoadResult = loadCounterHistory(meter.id)
                if (historyLoadResult.isFailure) {
                    println("⚠️ [ProfileDetailViewModel] Не удалось загрузить историю, валидации не будет")
                }

                // ШАГ 5: Валидация через интерфейс ValidateReading
                if (connector is ValidateReading) {
                    println("✅ [ProfileDetailViewModel] Провайдер поддерживает валидацию")
                    val minValueResult = connector.getMinimumAllowedValue(
                        counterId = apiCounterId,
                        accountNumber = account.accountNumber,
                        regionId = account.regionId?.toString(),
                        cacheData = cacheData
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
                    }

                    minValueResult.onFailure { error ->
                        println("⚠️ [ProfileDetailViewModel] Ошибка валидации: ${error.message}")
                    }
                } else {
                    println("⚠️ [ProfileDetailViewModel] Провайдер не поддерживает ValidateReading")
                }

                // ШАГ 6: Отправляем показание через коннектор
                println("📤 [ProfileDetailViewModel] Отправка через ${connector.javaClass.simpleName}")
                val result = connector.submitReading(
                    counterId = apiCounterId,
                    accountNumber = account.accountNumber,
                    value = newValue.toString(),
                    valueNight = null,
                    regionId = account.regionId?.toString(),
                    cacheData = cacheData
                )

                // ШАГ 7: Обрабатываем результат отправки
                if (result.isFailure) {
                    throw result.exceptionOrNull()
                        ?: Exception("Не удалось передать показание")
                }

                println("✅ [ProfileDetailViewModel] Показание успешно передано")

                // ✅ ШАГ 8: СБРАСЫВАЕМ историю для этого счётчика (чтобы перезагрузить с актуальными данными)
                println("🗑️ [ProfileDetailViewModel] Очищаем историю для ${meter.id} для обновления с сервера")
                _counterHistories.value = _counterHistories.value - meter.id

                // ШАГ 9: Перезагружаем счётчики для обновления UI
                _accounts.value.let { accounts ->
                    if (accounts.isNotEmpty()) {
                        println("🔄 [ProfileDetailViewModel] Перезагружаем счётчики...")
                        loadMetersForAllAccounts(accounts)

                        // ✅ НОВОЕ: Перезагружаем историю ПОСЛЕ обновления счётчиков
                        println("📊 [ProfileDetailViewModel] Перезагружаем историю для ${meter.id}...")
                        loadCounterHistory(meter.id)
                    }
                }

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
                // Убираем счётчик из списка "отправляется"
                _submittingMeters.value = _submittingMeters.value - meter.id
            }
        }
    }

    /**
     * Загружает историю показаний для конкретного счётчика
     *
     * Логика:
     * 1. Проверяет, не загружена ли уже история для этого счётчика
     * 2. Если загружена — ничего не делает (используем кеш из памяти)
     * 3. Если нет — делает запрос через коннектор провайдера
     * 4. ДОБАВЛЯЕТ результат в состояние, НЕ перезаписывая другие счётчики
     *
     * Вызывается:
     * - Перед отправкой показания (для валидации)
     * - При открытии модального окна истории
     *
     * @param meterId UI ID счётчика (формат: "accountId_apiCounterId")
     * @return Result с историей или ошибкой
     */
    suspend fun loadCounterHistory(meterId: String): Result<List<GetCounterHistory.HistoryEntry>> {
        return try {
            println("🔍 [ProfileDetailViewModel] Запрос истории для счётчика $meterId")

            // ШАГ 1: Проверяем, не загружена ли уже история
            if (_counterHistories.value.containsKey(meterId)) {
                val cachedHistory = _counterHistories.value[meterId]!!
                println("✅ [ProfileDetailViewModel] История уже загружена (${cachedHistory.size} записей)")
                return Result.success(cachedHistory)
            }

            // ШАГ 2: Находим счётчик и аккаунт
            val meter = _meters.value.firstOrNull { it.id == meterId }
                ?: return Result.failure(Exception("Счётчик не найден"))

            val account = _accounts.value.firstOrNull { it.id == meter.accountId }
                ?: return Result.failure(Exception("Аккаунт не найден"))

            // ШАГ 3: Получаем коннектор провайдера
            val connector = providerConnectorFactory.getConnector(account.providerId)

            // ШАГ 4: Проверяем, поддерживает ли провайдер получение истории
            if (connector !is GetCounterHistory) {
                println("⚠️ [ProfileDetailViewModel] Провайдер ${account.providerId} не поддерживает историю")
                return Result.failure(Exception("Провайдер не поддерживает загрузку истории"))
            }

            // ШАГ 5: Берём кеш и API ID из памяти
            val cacheData = _providerCache.value[account.id]?.rawData
            val apiCounterId = _providerCache.value[account.id]?.meterApiIds?.get(meterId)
                ?: return Result.failure(Exception("API ID счётчика не найден. Обновите страницу."))

            println("📋 [ProfileDetailViewModel] API ID: $apiCounterId")
            println("📦 [ProfileDetailViewModel] Кеш: ${if (cacheData != null) "ЕСТЬ" else "НЕТ"}")

            // ШАГ 6: Загружаем историю через коннектор
            val historyResult = connector.getCounterHistory(
                counterId = apiCounterId,
                accountNumber = account.accountNumber,
                regionId = account.regionId?.toString(),
                cacheData = cacheData
            )

            // ШАГ 7: Обрабатываем результат
            historyResult.fold(
                onSuccess = { history ->
                    println("✅ [ProfileDetailViewModel] Загружено ${history.size} записей истории")

                    // ✅ ВАЖНО: ДОБАВЛЯЕМ к существующим, НЕ перезаписываем
                    _counterHistories.value = _counterHistories.value + mapOf(meterId to history)

                    Result.success(history)
                },
                onFailure = { error ->
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
    suspend fun getMinimumValueForMeter(meterId: String): Int? {
        return try {
            println("🔍 [ProfileDetailViewModel] Запрос минимума для UI: $meterId")

            // ШАГ 1: Проверяем, есть ли история в состоянии
            if (!_counterHistories.value.containsKey(meterId)) {
                println("⚠️ [ProfileDetailViewModel] История для $meterId не загружена, валидация в UI невозможна")
                return null
            }

            println("✅ [ProfileDetailViewModel] История найдена, получаем минимум через ValidateReading")

            // ШАГ 2: Находим счётчик и аккаунт
            val meter = _meters.value.firstOrNull { it.id == meterId }
            if (meter == null) {
                println("❌ [ProfileDetailViewModel] Счётчик $meterId не найден")
                return null
            }

            val account = _accounts.value.firstOrNull { it.id == meter.accountId }
            if (account == null) {
                println("❌ [ProfileDetailViewModel] Аккаунт ${meter.accountId} не найден")
                return null
            }

            // ШАГ 3: Получаем коннектор
            val connector = providerConnectorFactory.getConnector(account.providerId)
            if (connector !is ValidateReading) {
                println("⚠️ [ProfileDetailViewModel] Провайдер ${account.providerId} не поддерживает ValidateReading")
                return null
            }

            // ШАГ 4: Берём кеш и API ID
            val cacheData = _providerCache.value[account.id]?.rawData
            val apiCounterId = _providerCache.value[account.id]?.meterApiIds?.get(meterId)
            if (apiCounterId == null) {
                println("❌ [ProfileDetailViewModel] API ID для $meterId не найден")
                return null
            }

            println("📋 [ProfileDetailViewModel] API ID: $apiCounterId, получаем минимум...")

            // ШАГ 5: Получаем минимум через ValidateReading
            val minValueResult = connector.getMinimumAllowedValue(
                counterId = apiCounterId,
                accountNumber = account.accountNumber,
                regionId = account.regionId?.toString(),
                cacheData = cacheData
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

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                accountRepository.deleteAccount(accountId)
                println("✅ [ProfileDetailViewModel] Аккаунт удалён")
            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка удаления: ${e.message}")
                _error.value = "Не удалось удалить аккаунт"
            }
        }
    }

    /**
     * Обновление периодов передачи для всех провайдеров
     */
    private suspend fun updateTransmissionPeriods(accounts: List<AccountDomainModel>) {
        val providerIds = accounts.map { it.providerId }.toSet()

        for (providerId in providerIds) {
            try {
                val connector = providerConnectorFactory.getConnector(providerId)

                // Проверяем, поддерживает ли провайдер получение периода
                if (connector !is GetTransmissionPeriod) {
                    println("⚠️ [ProfileDetailViewModel] Провайдер $providerId не поддерживает получение периода")
                    continue
                }

                // Находим первый аккаунт этого провайдера
                val account = accounts.firstOrNull { it.providerId == providerId } ?: continue

                // Проверяем, загружен ли период для текущего месяца
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                val provider = profileRepository.getProviderById(providerId).first()
                if (provider?.periodLoadedForMonth == currentMonth) {
                    println("✅ [ProfileDetailViewModel] Период для провайдера $providerId уже загружен")
                    continue
                }

                // Загружаем период
                val periodResult = connector.getTransmissionPeriod(
                    accountNumber = account.accountNumber,
                    regionId = account.regionId
                )

                periodResult.onSuccess { period ->
                    profileRepository.updateProviderTransmissionPeriod(
                        providerId = providerId,
                        periodStartDay = period.startDay,
                        periodEndDay = period.endDay
                    )
                    println("✅ [ProfileDetailViewModel] Период для провайдера $providerId: ${period.startDay}-${period.endDay}")
                }
            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка обновления периода для $providerId: ${e.message}")
            }
        }
    }

    // ============================================
    // LIFECYCLE
    // ============================================

    override fun onCleared() {
        super.onCleared()
        println("🧹 [ProfileDetailViewModel] ViewModel очищается → счётчики обнуляются")
    }
}
