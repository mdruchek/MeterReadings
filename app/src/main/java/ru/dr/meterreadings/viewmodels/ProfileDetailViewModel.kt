// app/src/main/java/ru/dr/meterreadings/viewmodels/ProfileDetailViewModel.kt

package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.mappers.KvcMeterMapper
import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
import ru.dr.meterreadings.data.remote.dto.KvcLocationDto
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProfileRepository  // ✅ ДОБАВИТЬ
import ru.dr.meterreadings.data.repository.providers.kvc.KvcRepository
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel  // ✅ ДОБАВИТЬ
import ru.dr.meterreadings.models.ui.MeterUiModel
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,  // ✅ ДОБАВИТЬ
    private val kvcRepository: KvcRepository
) : ViewModel() {

    // ============================================
    // STATE
    // ============================================

    private val _profileId = MutableStateFlow<String?>(null)
    val profileId: StateFlow<String?> = _profileId.asStateFlow()

    // ✅ ДОБАВИТЬ: Профиль
    private val _profile = MutableStateFlow<ProfileDomainModel?>(null)
    val profile: StateFlow<ProfileDomainModel?> = _profile.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountDomainModel>>(emptyList())
    val accounts: StateFlow<List<AccountDomainModel>> = _accounts.asStateFlow()

    private val _accountAddresses = MutableStateFlow<Map<String, String>>(emptyMap())
    val accountAddresses: StateFlow<Map<String, String>> = _accountAddresses.asStateFlow()

    private val _meters = MutableStateFlow<List<MeterUiModel>>(emptyList())
    val meters: StateFlow<List<MeterUiModel>> = _meters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _submittingMeters = MutableStateFlow<Set<String>>(emptySet())
    val submittingMeters: StateFlow<Set<String>> = _submittingMeters.asStateFlow()

    private val _kvcDataCache = MutableStateFlow<Map<String, KvcCachedData>>(emptyMap())

    // ============================================
    // PUBLIC METHODS
    // ============================================

    fun initialize(profileId: String) {
        _profileId.value = profileId

        // ✅ ДОБАВИТЬ: Загружаем профиль
        viewModelScope.launch {
            profileRepository.getProfileById(profileId).collect { profile ->
                _profile.value = profile
            }
        }

        // Подписываемся на изменения аккаунтов
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
                            "1" -> {  // КВЦ
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
        println("=" .repeat(60))
        println("🔍 [loadKvcMeters] НАЧАЛО загрузки для аккаунта")
        println("   Account ID: ${account.id}")
        println("   Account Number: ${account.accountNumber}")
        println("   Provider ID: ${account.providerId}")
        println("   Region ID: ${account.regionId}")
        println("=" .repeat(60))

        val regionId = account.regionId
            ?: throw Exception("Для аккаунта КВЦ не указан регион")

        println("📡 [loadKvcMeters] ШАГ 1: Загрузка конфигураций БД для региона $regionId")
        val locationsResult = kvcRepository.getLocationsForRegion(regionId)

        if (locationsResult.isFailure) {
            val error = locationsResult.exceptionOrNull()
            println("❌ [loadKvcMeters] ШАГ 1 FAILED: ${error?.message}")
            error?.printStackTrace()
            throw error ?: Exception("Не удалось загрузить конфигурации БД")
        }

        val locations = locationsResult.getOrThrow()
        println("✅ [loadKvcMeters] ШАГ 1 OK: Конфигураций БД: ${locations.size}")
        locations.forEachIndexed { index, location ->
            println("   Location[$index]: dbName=${location.dbName}, idTer=${location.dbName}")
        }

        println("📡 [loadKvcMeters] ШАГ 2: Поиск абонента ${account.accountNumber}")
        val abonentResult = kvcRepository.getAbonentInfo(
            locations = locations,
            accountNumber = account.accountNumber,
            target = 0
        )

        if (abonentResult.isFailure) {
            val error = abonentResult.exceptionOrNull()
            println("❌ [loadKvcMeters] ШАГ 2 FAILED: ${error?.message}")
            error?.printStackTrace()
            throw error ?: Exception("Абонент не найден")
        }

        val abonentInfo = abonentResult.getOrThrow()
        val address = abonentInfo.getFullAddress()
        println("✅ [loadKvcMeters] ШАГ 2 OK: Абонент найден")
        println("   Адрес: $address")
        println("   ФИО: ${abonentInfo.fio}")
        println("   Location: dbName=${abonentInfo.location.dbName}")

        println("📡 [loadKvcMeters] ШАГ 3: Загрузка счётчиков")
        val countersResult = kvcRepository.getCounters(
            location = abonentInfo.location,
            accountNumber = account.accountNumber
        )

        if (countersResult.isFailure) {
            val error = countersResult.exceptionOrNull()
            println("❌ [loadKvcMeters] ШАГ 3 FAILED: ${error?.message}")
            error?.printStackTrace()
            throw error ?: Exception("Не удалось загрузить счётчики")
        }

        val kvcCounters = countersResult.getOrThrow()
        println("✅ [loadKvcMeters] ШАГ 3 OK: Счётчиков от API: ${kvcCounters.size}")
        kvcCounters.forEachIndexed { index, counter ->
            println("   Counter[$index]:")
            println("     ID: ${counter.idCnt}")
            println("     Тип: ${counter.servName}")
            println("     Номер: ${counter.number}")
            println("     Можно редактировать: ${counter.canEdit()}")
            println("     Последнее значение: ${counter.cValLst}")
            println("     Дата: ${counter.datLst}")
        }

        println("🔄 [loadKvcMeters] ШАГ 4: Маппинг в UI модели")
        val uiMeters = KvcMeterMapper.mapListToUi(
            kvcCounters = kvcCounters,
            accountId = account.id
        )
        println("✅ [loadKvcMeters] ШАГ 4 OK: UI моделей создано: ${uiMeters.size}")
        uiMeters.forEachIndexed { index, meter ->
            println("   Meter[$index]: ${meter.type}, lastValue=${meter.lastValue}")
        }

        kvcCache[account.id] = KvcCachedData(
            location = abonentInfo.location,
            counters = kvcCounters
        )

        println("=" .repeat(60))
        println("✅ [loadKvcMeters] ЗАВЕРШЕНО успешно")
        println("=" .repeat(60))

        return Pair(uiMeters, address)
    }

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

    private data class KvcCachedData(
        val location: KvcLocationDto,
        val counters: List<KvcCounterDto>
    )
}
