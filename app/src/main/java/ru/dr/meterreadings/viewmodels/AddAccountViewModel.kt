package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.domain.connector.HasRegions  // ← ДОБАВИТЬ
import ru.dr.meterreadings.domain.connector.SearchAccount  // ← ДОБАВИТЬ
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val connectorFactory: ProviderConnectorFactory
) : ViewModel() {

    // =====================================================
    // МОДЕЛЬ ОШИБКИ
    // =====================================================

    data class ErrorState(
        val title: String,
        val message: String
    )

    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

    private val _shouldResetToStep1 = MutableStateFlow(false)
    val shouldResetToStep1: StateFlow<Boolean> = _shouldResetToStep1.asStateFlow()

    fun showError(title: String, message: String) {
        _errorState.value = ErrorState(title, message)
        _shouldResetToStep1.value = true
        println("❌ [AddAccountVM] Ошибка: $title - $message")
    }

    fun dismissError() {
        _errorState.value = null
    }

    fun resetCompleted() {
        _shouldResetToStep1.value = false
        println("🔄 [AddAccountVM] Сброс завершён")
    }

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
    }

    fun getSelectedProvider(): StateFlow<ProviderDomainModel?> {
        return combine(allProviders, selectedProviderId) { providers, selectedId ->
            if (selectedId == null) null
            else providers.find { it.id == selectedId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun clearSelection() {
        _selectedProviderId.value = null
        _regions.value = emptyList()
        _selectedRegionId.value = null
        _providerHasRegions.value = false
        println("🔄 [AddAccountVM] Выбор сброшен")
    }

    // =====================================================
    // STATE - регионы провайдера
    // =====================================================

    private val _regions = MutableStateFlow<List<HasRegions.RegionInfo>>(emptyList())
    val regions: StateFlow<List<HasRegions.RegionInfo>> = _regions.asStateFlow()

    private val _providerHasRegions = MutableStateFlow(false)
    val providerHasRegions: StateFlow<Boolean> = _providerHasRegions.asStateFlow()

    private val _isLoadingRegions = MutableStateFlow(false)
    val isLoadingRegions: StateFlow<Boolean> = _isLoadingRegions.asStateFlow()

    /**
     * Загрузка регионов для выбранного провайдера
     *
     * ✅ Вызывается из UI при переходе на шаг 2
     */
    fun loadRegionsForProvider(providerId: String) {  // ← Убрали private
        viewModelScope.launch {
            _isLoadingRegions.value = true
            _regions.value = emptyList()

            try {
                val connector = connectorFactory.getConnector(providerId.toLong())

                if (connector is HasRegions) {
                    println("🔍 [AddAccountVM] Провайдер поддерживает регионы, загружаем...")

                    val result = connector.getRegions()

                    result.onSuccess { regionsList ->
                        _regions.value = regionsList
                        _providerHasRegions.value = regionsList.isNotEmpty()
                        println("✅ [AddAccountVM] Загружено регионов: ${regionsList.size}")
                    }.onFailure { error ->
                        handleLoadRegionsError(error)
                    }
                } else {
                    println("ℹ️ [AddAccountVM] Провайдер НЕ поддерживает регионы")
                    _providerHasRegions.value = false
                }
            } catch (e: Exception) {
                handleLoadRegionsError(e)
            } finally {
                _isLoadingRegions.value = false
            }
        }
    }

    private fun handleLoadRegionsError(error: Throwable) {
        val message = when (error) {
            is SocketTimeoutException ->
                "Сервер не отвечает. Проверьте подключение к интернету или попробуйте позже."

            is ConnectException, is UnknownHostException ->
                "Не удалось подключиться к серверу. Проверьте подключение к интернету."

            is IOException ->
                "Ошибка сети: ${error.message ?: "Неизвестная ошибка"}"

            else ->
                "Не удалось загрузить данные: ${error.message ?: "Неизвестная ошибка"}"
        }

        showError(
            title = "Ошибка подключения",
            message = message
        )
    }

    // =====================================================
    // STATE - выбранный регион
    // =====================================================

    private val _selectedRegionId = MutableStateFlow<String?>(null)
    val selectedRegionId: StateFlow<String?> = _selectedRegionId.asStateFlow()

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

    /**
     * Поиск адреса по лицевому счёту
     */
    fun searchAccountAddress(
        providerId: String,
        accountNumber: String,
        regionId: String?
    ) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchedAddress.value = null

            try {
                val connector = connectorFactory.getConnector(providerId.toLong())

                // ✅ Проверяем: поддерживает ли провайдер поиск
                if (connector is SearchAccount) {
                    println("🔍 [AddAccountVM] Поиск адреса...")

                    val result = connector.searchAccount(
                        accountNumber = accountNumber,
                        regionId = regionId
                    )

                    result.onSuccess { address ->
                        _searchedAddress.value = address
                        println("✅ [AddAccountVM] Адрес найден: $address")
                    }.onFailure { error ->
                        showError(
                            title = "Абонент не найден",
                            message = "Лицевой счёт $accountNumber не найден в системе"
                        )
                    }
                } else {
                    showError(
                        title = "Ошибка",
                        message = "Провайдер не поддерживает поиск адреса"
                    )
                }
            } catch (e: Exception) {
                showError(
                    title = "Ошибка поиска",
                    message = when (e) {
                        is SocketTimeoutException ->
                            "Сервер не отвечает. Попробуйте позже."
                        else ->
                            "Не удалось выполнить поиск: ${e.message}"
                    }
                )
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResult() {
        _searchedAddress.value = null
        println("🔄 [AddAccountVM] Результат поиска очищен")
    }
}
