// app/src/main/java/ru/dr/meterreadings/viewmodels/ProfileDetailViewModel.kt
package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.local.dao.MeterDao
import ru.dr.meterreadings.data.mappers.UniversalMeterMapper
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.domain.connector.GetTransmissionPeriod
import ru.dr.meterreadings.domain.connector.LoadMeters
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.domain.connector.SubmitReadings
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.ui.MeterUiModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel для экрана детализации профиля (ProfileDetailScreen).
 *
 * Управляет:
 * - Загрузкой и отображением списка аккаунтов (ЛС) профиля
 * - Загрузкой счётчиков по каждому аккаунту (универсально через коннекторы)
 * - Отправкой показаний через API провайдера
 * - Кешированием данных для работы без повторных запросов
 */
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val providerConnectorFactory: ProviderConnectorFactory,
    private val meterDao: MeterDao
) : ViewModel() {

    // ============================================
    // STATE (Состояние экрана)
    // ============================================

    /** ID текущего профиля */
    private val _profileId = MutableStateFlow<String?>(null)
    val profileId: StateFlow<String?> = _profileId.asStateFlow()

    /** Данные текущего профиля */
    private val _profile = MutableStateFlow<ProfileDomainModel?>(null)
    val profile: StateFlow<ProfileDomainModel?> = _profile.asStateFlow()

    /** Список аккаунтов (ЛС) для профиля */
    private val _accounts = MutableStateFlow<List<AccountDomainModel>>(emptyList())
    val accounts: StateFlow<List<AccountDomainModel>> = _accounts.asStateFlow()

    /** Карта: id аккаунта → строка адреса абонента */
    private val _accountAddresses = MutableStateFlow<Map<String, String>>(emptyMap())
    val accountAddresses: StateFlow<Map<String, String>> = _accountAddresses.asStateFlow()

    /** Список UI‑моделей счётчиков для отображения */
    private val _meters = MutableStateFlow<List<MeterUiModel>>(emptyList())
    val meters: StateFlow<List<MeterUiModel>> = _meters.asStateFlow()

    /** Флаг глобальной загрузки */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Текст ошибки для показа в UI */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Множество ID счётчиков, по которым сейчас отправляется показание */
    private val _submittingMeters = MutableStateFlow<Set<String>>(emptySet())
    val submittingMeters: StateFlow<Set<String>> = _submittingMeters.asStateFlow()

    /** Универсальный кеш данных провайдеров (для submitReading) */
    private val _providerCache = MutableStateFlow<Map<String, ProviderCacheData>>(emptyMap())

    // ============================================
    // PUBLIC METHODS (Публичные методы)
    // ============================================

    /**
     * Инициализирует экран детализации профиля
     */
    fun initialize(profileId: String) {
        _profileId.value = profileId

        // Подписка на изменения данных профиля
        viewModelScope.launch {
            profileRepository.getProfileById(profileId).collect { profile ->
                _profile.value = profile
            }
        }

        // Подписка на изменения списка аккаунтов профиля
        viewModelScope.launch {
            accountRepository.getAccountsByProfileId(profileId).collect { accounts ->
                println("🔍 [ProfileDetailViewModel] Обновление аккаунтов: ${accounts.size}")
                _accounts.value = accounts
                if (accounts.isNotEmpty()) {
                    loadMetersForAllAccounts(accounts)
                } else {
                    _meters.value = emptyList()
                    _accountAddresses.value = emptyMap()
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Отправляет новое показание по конкретному счётчику
     */
    fun submitReading(meter: MeterUiModel, newValue: Int) {
        viewModelScope.launch {
            _submittingMeters.value = _submittingMeters.value + meter.id
            try {
                println("📤 [ProfileDetailViewModel] Отправляем показание: ${meter.type} = $newValue")

                // Находим аккаунт
                val account = _accounts.value.firstOrNull { it.id == meter.accountId }
                    ?: throw Exception("Аккаунт не найден")

                // Получаем коннектор
                val connector = providerConnectorFactory.getConnector(account.providerId)

                if (connector !is SubmitReadings) {
                    throw Exception("Провайдер не поддерживает отправку показаний")
                }

                // ✅ ИЗМЕНЕНО: Берём кеш из памяти
                val cacheData = _providerCache.value[account.id]?.rawData

                // ✅ ИЗМЕНЕНО: Берём API ID из кеша
                val apiCounterId = _providerCache.value[account.id]?.meterApiIds?.get(meter.id)
                    ?: throw Exception("Данные счётчика не загружены. Обновите страницу.")

                println("📋 [ProfileDetailViewModel] API ID счётчика: $apiCounterId")
                println("📦 [ProfileDetailViewModel] Кеш: ${if (cacheData != null) "ЕСТЬ" else "НЕТ"}")

                // ✅ ИЗМЕНЕНО: Передаём кеш в submitReading
                val result = connector.submitReading(
                    counterId = apiCounterId,
                    accountNumber = account.accountNumber,
                    value = newValue.toString(),
                    valueNight = null,
                    regionId = account.regionId?.toString(),
                    cacheData = cacheData  // ✅ НОВОЕ: передаём кеш
                )

                if (result.isFailure) {
                    throw result.exceptionOrNull()
                        ?: Exception("Не удалось передать показание")
                }

                println("✅ [ProfileDetailViewModel] Показание успешно передано")

                // Обновляем дату передачи в БД
                val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(Calendar.getInstance().time)
                meterDao.updateSubmissionDate(
                    meterId = meter.id,
                    date = today
                )

                println("✅ [submitReading] Дата передачи обновлена в БД: $today")

                // Перезагружаем счётчики
                _accounts.value.let { accounts ->
                    if (accounts.isNotEmpty()) {
                        loadMetersForAllAccounts(accounts)
                    }
                }

            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Ошибка передачи: ${e.message}")
                e.printStackTrace()
                _error.value = when {
                    e.message?.contains("Период") == true -> e.message
                    e.message?.contains("Передача доступна") == true -> e.message
                    else -> "Не удалось передать показание: ${e.message}"
                }
            } finally {
                _submittingMeters.value = _submittingMeters.value - meter.id
            }
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

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        _accounts.value.let { accounts ->
            if (accounts.isNotEmpty()) {
                loadMetersForAllAccounts(accounts)
            }
        }
    }

    // ============================================
    // PRIVATE METHODS (Внутренние методы)
    // ============================================

    /**
     * Загружает счётчики для всех аккаунтов (универсально через коннекторы)
     */
    private fun loadMetersForAllAccounts(accounts: List<AccountDomainModel>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val allMeters = mutableListOf<MeterUiModel>()
            val providerCache = mutableMapOf<String, ProviderCacheData>()
            val addresses = mutableMapOf<String, String>()

            try {
                println("🔍 [ProfileDetailViewModel] Загружаем счётчики для ${accounts.size} аккаунтов")

                for (account in accounts) {
                    println("📋 [ProfileDetailViewModel] Аккаунт: ${account.accountNumber}, Провайдер: ${account.providerId}")

                    try {
                        // ✅ Получаем коннектор через фабрику
                        val connector = providerConnectorFactory.getConnector(account.providerId)

                        // ✅ Проверяем, поддерживает ли провайдер загрузку счётчиков
                        if (connector !is LoadMeters) {
                            println("⚠️ [ProfileDetailViewModel] Провайдер ${account.providerId} не поддерживает загрузку счётчиков")
                            continue
                        }

                        // ✅ Загружаем счётчики через универсальный интерфейс
                        val result = connector.loadMeters(
                            accountNumber = account.accountNumber,
                            regionId = account.regionId?.toString()
                        )

                        result.fold(
                            onSuccess = { loadResult ->
                                // Маппинг в UI
                                val uiMeters = UniversalMeterMapper.mapListToUi(
                                    meters = loadResult.meters,
                                    accountId = account.id
                                )
                                allMeters.addAll(uiMeters)
                                addresses[account.id] = loadResult.address

                                // Сохраняем в БД
                                val entities = UniversalMeterMapper.mapListToEntity(
                                    meters = loadResult.meters,
                                    accountId = account.id
                                )
                                meterDao.insertAll(entities)

                                // Создаём кеш для submitReading
                                val meterApiIds = loadResult.meters.associate {
                                    "${account.id}_${it.id}" to it.id
                                }
                                providerCache[account.id] = ProviderCacheData(
                                    meterApiIds = meterApiIds,
                                    rawData = loadResult.cacheData
                                )

                                println("✅ [ProfileDetailViewModel] Загружено ${uiMeters.size} счётчиков для ЛС ${account.accountNumber}")
                            },
                            onFailure = { error ->
                                println("❌ [ProfileDetailViewModel] Ошибка для ЛС ${account.accountNumber}: ${error.message}")
                            }
                        )
                    } catch (e: Exception) {
                        println("❌ [ProfileDetailViewModel] Ошибка для аккаунта ${account.accountNumber}: ${e.message}")
                    }
                }

                _meters.value = allMeters
                _accountAddresses.value = addresses
                _providerCache.value = providerCache

                // Обновляем периоды передачи
                updateTransmissionPeriods(accounts)

                println("✅ [ProfileDetailViewModel] ИТОГО загружено счётчиков: ${allMeters.size}")
            } catch (e: Exception) {
                println("❌ [ProfileDetailViewModel] Общая ошибка: ${e.message}")
                e.printStackTrace()
                _error.value = "Не удалось загрузить счётчики: ${e.message}"
            } finally {
                _isLoading.value = false
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
    // DATA CLASSES
    // ============================================

    /**
     * Универсальный кеш данных провайдера
     */
    private data class ProviderCacheData(
        val meterApiIds: Map<String, String>,  // UI ID → API ID счётчика
        val rawData: Any? = null  // Опционально: специфичные данные провайдера
    )
}
