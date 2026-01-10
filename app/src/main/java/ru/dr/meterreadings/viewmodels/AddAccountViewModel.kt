package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.domain.connector.HasRegions
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.domain.connector.SearchAccount
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import javax.inject.Inject

/**
 * ViewModel для мастера добавления лицевого счёта
 *
 * Управляет:
 * - Загрузкой списка провайдеров из БД
 * - Поиском провайдеров по названию
 * - Выбором провайдера пользователем
 * - Загрузкой регионов (для провайдеров с HasRegions)
 * - Поиском адреса по лицевому счёту
 */
@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val connectorFactory: ProviderConnectorFactory
) : ViewModel() {

    // =====================================================
    // STATE - поисковый запрос
    // =====================================================

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        println("🔍 [AddAccountVM] Поиск: '$query'")
    }

    // =====================================================
    // STATE - список провайдеров (с фильтрацией)
    // =====================================================

    private val allProviders: StateFlow<List<ProviderDomainModel>> =
        providerRepository.getAllProviders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val filteredProviders: StateFlow<List<ProviderUiModel>> = combine(
        allProviders,
        searchQuery
    ) { providers, query ->
        println("🔄 [AddAccountVM] Фильтрация: ${providers.size} провайдеров, запрос: '$query'")

        if (query.isBlank()) {
            providers.map { ProviderUiModel(it) }
        } else {
            val filtered = providers.filter { provider ->
                provider.name.contains(query, ignoreCase = true)
            }
            println("✅ [AddAccountVM] Найдено: ${filtered.size}")
            filtered.map { ProviderUiModel(it) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // =====================================================
    // STATE - выбранный провайдер
    // =====================================================

    private val _selectedProviderId = MutableStateFlow<String?>(null)
    val selectedProviderId: StateFlow<String?> = _selectedProviderId.asStateFlow()

    fun selectProvider(providerId: String) {
        _selectedProviderId.value = providerId
        println("✅ [AddAccountVM] Выбран провайдер: $providerId")

        // Автоматически загружаем регионы если нужно
        loadRegionsForProvider(providerId)
    }

    fun getSelectedProvider(): StateFlow<ProviderDomainModel?> {
        return combine(allProviders, selectedProviderId) { providers, selectedId ->
            if (selectedId == null) {
                null
            } else {
                providers.find { it.id == selectedId }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    // =====================================================
    // STATE - регионы (для провайдеров с HasRegions)
    // =====================================================

    private val _regions = MutableStateFlow<List<HasRegions.RegionInfo>>(emptyList())
    val regions: StateFlow<List<HasRegions.RegionInfo>> = _regions.asStateFlow()

    private val _selectedRegionId = MutableStateFlow<String?>(null)
    val selectedRegionId: StateFlow<String?> = _selectedRegionId.asStateFlow()

    private val _providerHasRegions = MutableStateFlow(false)
    val providerHasRegions: StateFlow<Boolean> = _providerHasRegions.asStateFlow()

    private fun loadRegionsForProvider(providerId: String) {
        viewModelScope.launch {
            try {
                val connector = connectorFactory.getConnector(providerId.toLong())

                if (connector is HasRegions) {
                    println("✅ [AddAccountVM] Провайдер имеет регионы")
                    _providerHasRegions.value = true

                    connector.getRegions().onSuccess { regionList ->
                        _regions.value = regionList
                        println("✅ [AddAccountVM] Загружено регионов: ${regionList.size}")
                    }.onFailure { error ->
                        println("❌ [AddAccountVM] Ошибка загрузки регионов: ${error.message}")
                        _providerHasRegions.value = false
                    }
                } else {
                    println("ℹ️ [AddAccountVM] Провайдер без регионов")
                    _providerHasRegions.value = false
                    _regions.value = emptyList()
                }
            } catch (e: Exception) {
                println("❌ [AddAccountVM] Ошибка: ${e.message}")
                _providerHasRegions.value = false
                _regions.value = emptyList()
            }
        }
    }

    fun selectRegion(regionId: String) {
        _selectedRegionId.value = regionId
        println("✅ [AddAccountVM] Выбран регион: $regionId")
    }

    // =====================================================
    // STATE - поиск адреса
    // =====================================================

    private val _searchedAddress = MutableStateFlow<String?>(null)
    val searchedAddress: StateFlow<String?> = _searchedAddress.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun searchAccountAddress(providerId: String, accountNumber: String, regionId: String?) {
        viewModelScope.launch {
            _isSearching.value = true

            try {
                val connector = connectorFactory.getConnector(providerId.toLong())

                if (connector is SearchAccount) {
                    connector.searchAccount(accountNumber, regionId).onSuccess { address ->
                        _searchedAddress.value = address
                        println("✅ [AddAccountVM] Найден адрес: $address")
                    }.onFailure { error ->
                        _searchedAddress.value = null
                        println("❌ [AddAccountVM] Ошибка поиска: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                _searchedAddress.value = null
                println("❌ [AddAccountVM] Ошибка: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }
}
