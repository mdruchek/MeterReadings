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
import ru.dr.meterreadings.data.mappers.KvcMeterMapper
import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
import ru.dr.meterreadings.data.remote.dto.KvcLocationDto
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.providers.kvc.KvcRepository
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.ui.MeterUiModel
// ✅ ЗАМЕНА java.time.* на старые классы
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel для экрана детализации профиля (ProfileDetailScreen).
 *
 * Управляет:
 * - Загрузкой и отображением списка аккаунтов (ЛС) профиля
 * - Загрузкой счётчиков по каждому аккаунту
 * - Отправкой показаний через API провайдера
 * - Кешированием данных КВЦ для работы без повторных запросов
 */
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val kvcRepository: KvcRepository,
    private val meterDao: MeterDao
) : ViewModel() {

    // ============================================
    // STATE (Состояние экрана)
    // ============================================

    /** ID текущего профиля, выбранного на экране */
    private val _profileId = MutableStateFlow<String?>(null)
    val profileId: StateFlow<String?> = _profileId.asStateFlow()

    /** Данные текущего профиля (название, иконка и т.п.) */
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

    /** Флаг глобальной загрузки (показывается индикатор на экране) */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Текст ошибки для показа в UI (Snackbar/диалог) */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Множество ID счётчиков, по которым сейчас отправляется показание */
    private val _submittingMeters = MutableStateFlow<Set<String>>(emptySet())
    val submittingMeters: StateFlow<Set<String>> = _submittingMeters.asStateFlow()

    /** Кеш данных КВЦ по аккаунтам: id аккаунта → location + список счётчиков */
    private val _kvcDataCache = MutableStateFlow<Map<String, KvcCachedData>>(emptyMap())

    // ============================================
    // PUBLIC METHODS (Публичные методы)
    // ============================================

    /**
     * Инициализирует экран детализации профиля:
     * 1) Сохраняет ID профиля
     * 2) Подписывается на изменения профиля из БД
     * 3) Подписывается на список аккаунтов и при их наличии загружает счётчики
     *
     * @param profileId UUID профиля
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
                    // Если аккаунты есть — грузим счётчики и адреса
                    loadMetersForAllAccounts(accounts)
                } else {
                    // Если аккаунтов нет — очищаем состояние
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

                val kvcData = _kvcDataCache.value[meter.accountId]
                    ?: throw Exception("Данные КВЦ не загружены. Обновите страницу.")

                val counterId = meter.id.substringAfterLast("_").toInt()
                val kvcCounter = kvcData.counters.find { it.idCnt == counterId }
                    ?: throw Exception("Счётчик не найден")

                val result = kvcRepository.submitReading(
                    counter = kvcCounter,
                    location = kvcData.location,
                    value = newValue.toString(),
                    valueNight = null
                )

                if (result.isFailure) {
                    throw result.exceptionOrNull()
                        ?: Exception("Не удалось передать показание")
                }

                println("✅ [ProfileDetailViewModel] Показание успешно передано")

                // ✅ ИСПОЛЬЗУЕМ SimpleDateFormat вместо LocalDate
                val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                meterDao.updateSubmissionDate(
                    meterId = meter.id,
                    date = today
                )

                println("✅ [submitReading] Дата передачи обновлена в БД: $today")

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

    private fun loadMetersForAllAccounts(accounts: List<AccountDomainModel>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val allMeters = mutableListOf<MeterUiModel>()
            val kvcCache = mutableMapOf<String, KvcCachedData>()
            val addresses = mutableMapOf<String, String>()

            try {
                println("🔍 [ProfileDetailViewModel] Загружаем счётчики для ${accounts.size} аккаунтов")

                for (account in accounts) {
                    println("📋 [ProfileDetailViewModel] Аккаунт: ${account.accountNumber}, Провайдер: ${account.providerId}")

                    try {
                        when (account.providerId) {
                            "1" -> {
                                val (meters, address) = loadKvcMeters(account, kvcCache)
                                allMeters.addAll(meters)
                                addresses[account.id] = address
                                println("✅ [ProfileDetailViewModel] Загружено ${meters.size} счётчиков КВЦ")
                                println("   📍 Адрес: $address")
                            }
                            else -> {
                                println("⚠️ [ProfileDetailViewModel] Провайдер ${account.providerId} пока не поддерживается")
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ [ProfileDetailViewModel] Ошибка для ЛС ${account.accountNumber}: ${e.message}")
                    }
                }

                _meters.value = allMeters
                _accountAddresses.value = addresses
                _kvcDataCache.value = kvcCache

                val providerIds = accounts.map { it.providerId }.toSet()
                for (providerId in providerIds) {
                    if (kvcCache.isNotEmpty()) {
                        updateProviderTransmissionPeriod(providerId)
                    }
                }

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

    private suspend fun loadKvcMeters(
        account: AccountDomainModel,
        kvcCache: MutableMap<String, KvcCachedData>
    ): Pair<List<MeterUiModel>, String> {
        println("=".repeat(60))
        println("🔍 [loadKvcMeters] НАЧАЛО загрузки для аккаунта")
        println("   Account ID: ${account.id}")
        println("   Account Number: ${account.accountNumber}")
        println("   Region ID: ${account.regionId}")
        println("=".repeat(60))

        val regionId = account.regionId
            ?: throw Exception("Для аккаунта КВЦ не указан регион")

        // ШАГ 1: Загружаем конфигурации БД
        println("📡 [loadKvcMeters] ШАГ 1: Загрузка конфигураций БД для региона $regionId")
        val locationsResult = kvcRepository.getLocationsForRegion(regionId)
        if (locationsResult.isFailure) {
            throw locationsResult.exceptionOrNull() ?: Exception("Не удалось загрузить конфигурации БД")
        }
        val locations = locationsResult.getOrThrow()
        println("✅ [loadKvcMeters] ШАГ 1 OK: Конфигураций БД: ${locations.size}")

        // ШАГ 2: Ищем абонента
        println("📡 [loadKvcMeters] ШАГ 2: Поиск абонента ${account.accountNumber}")
        val abonentResult = kvcRepository.getAbonentInfo(
            locations = locations,
            accountNumber = account.accountNumber,
            target = 0
        )
        if (abonentResult.isFailure) {
            throw abonentResult.exceptionOrNull() ?: Exception("Абонент не найден")
        }
        val abonentInfo = abonentResult.getOrThrow()
        val address = abonentInfo.getFullAddress()
        println("✅ [loadKvcMeters] ШАГ 2 OK: Абонент найден")
        println("   Адрес: $address")

        // ШАГ 3: Получаем счётчики
        println("📡 [loadKvcMeters] ШАГ 3: Загрузка счётчиков")
        val countersResult = kvcRepository.getCounters(
            location = abonentInfo.location,
            accountNumber = account.accountNumber
        )
        if (countersResult.isFailure) {
            throw countersResult.exceptionOrNull() ?: Exception("Не удалось загрузить счётчики")
        }
        val kvcCounters = countersResult.getOrThrow()
        println("✅ [loadKvcMeters] ШАГ 3 OK: Счётчиков от API: ${kvcCounters.size}")

        // ШАГ 4: Сохраняем в БД
        val meterEntities = KvcMeterMapper.mapListToEntity(
            kvcCounters = kvcCounters,
            accountId = account.id
        )
        meterDao.insertAll(meterEntities)
        println("✅ [loadKvcMeters] Счётчики сохранены в БД: ${meterEntities.size}")

        // ШАГ 5: Маппинг в UI
        println("🔄 [loadKvcMeters] ШАГ 4: Маппинг в UI модели")
        val uiMeters = KvcMeterMapper.mapListToUi(
            kvcCounters = kvcCounters,
            accountId = account.id
        )
        println("✅ [loadKvcMeters] ШАГ 4 OK: UI моделей создано: ${uiMeters.size}")

        // ШАГ 6: Кладём в кеш
        kvcCache[account.id] = KvcCachedData(
            location = abonentInfo.location,
            counters = kvcCounters
        )

        println("=".repeat(60))
        println("✅ [loadKvcMeters] ЗАВЕРШЕНО успешно")
        println("=".repeat(60))

        return Pair(uiMeters, address)
    }

    /**
     * Обновляет период передачи показаний для провайдера
     */
    private suspend fun updateProviderTransmissionPeriod(providerId: String) {
        try {
            // ✅ ИСПОЛЬЗУЕМ SimpleDateFormat вместо LocalDate
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(Calendar.getInstance().time)

            println("📅 [updateProviderTransmissionPeriod] Провайдер: $providerId, Месяц: $currentMonth")

            val provider = profileRepository.getProviderById(providerId).first()
            if (provider == null) {
                println("⚠️ [updateProviderTransmissionPeriod] Провайдер $providerId не найден")
                return
            }

            if (provider.periodLoadedForMonth == currentMonth) {
                println("✅ [updateProviderTransmissionPeriod] Период уже загружен для $currentMonth")
                println("   Период: ${provider.transmissionPeriodStartDay}-${provider.transmissionPeriodEndDay}")
                return
            }

            println("🔄 [updateProviderTransmissionPeriod] Период НЕ загружен, загружаем...")

            val kvcData = _kvcDataCache.value.values.firstOrNull()
            if (kvcData == null) {
                println("⚠️ [updateProviderTransmissionPeriod] Нет данных в кеше")
                return
            }

            val transitDaysResult = kvcRepository.getTransitDays(
                location = kvcData.location,
                accountNumber = _accounts.value.firstOrNull()?.accountNumber ?: ""
            )

            if (transitDaysResult.isFailure) {
                println("❌ [updateProviderTransmissionPeriod] Ошибка: ${transitDaysResult.exceptionOrNull()?.message}")
                return
            }

            val transitDays = transitDaysResult.getOrThrow()
            println("✅ [updateProviderTransmissionPeriod] Период получен: ${transitDays.first}-${transitDays.last}")

            profileRepository.updateProviderTransmissionPeriod(
                providerId = providerId,
                periodStartDay = transitDays.first,
                periodEndDay = transitDays.last
            )

            println("✅ [updateProviderTransmissionPeriod] Период сохранён для месяца $currentMonth")

        } catch (e: Exception) {
            println("❌ [updateProviderTransmissionPeriod] Ошибка: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Внутренняя структура для кеша данных КВЦ по аккаунту
     */
    private data class KvcCachedData(
        val location: KvcLocationDto,
        val counters: List<KvcCounterDto>
    )
}
