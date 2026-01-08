package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import ru.dr.meterreadings.data.repository.ProviderRepository
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
 */
@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    // =====================================================
    // STATE - поисковый запрос
    // =====================================================

    /**
     * Текст поискового запроса (введённый пользователем)
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Обновить поисковый запрос
     *
     * Вызывается при изменении текста в TextField
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        println("🔍 [AddAccountVM] Поиск: '$query'")
    }

    // =====================================================
    // STATE - список провайдеров (с фильтрацией)
    // =====================================================

    /**
     * Список всех провайдеров из БД
     *
     * Автоматически обновляется при изменениях в БД
     */
    private val allProviders: StateFlow<List<ProviderDomainModel>> =
        providerRepository.getAllProviders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Отфильтрованный список провайдеров (для отображения в UI)
     *
     * Реактивно фильтруется по searchQuery:
     * - Если поиск пустой → показываем всех
     * - Если есть текст → фильтруем по названию
     *
     * UI автоматически обновится при изменении searchQuery или allProviders
     */
    val filteredProviders: StateFlow<List<ProviderUiModel>> = combine(
        allProviders,
        searchQuery
    ) { providers, query ->
        println("🔄 [AddAccountVM] Фильтрация: ${providers.size} провайдеров, запрос: '$query'")

        if (query.isBlank()) {
            // Поиск пустой → показываем всех провайдеров
            providers.map { ProviderUiModel(it) }
        } else {
            // Фильтруем по названию (игнорируя регистр)
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

    /**
     * ID выбранного провайдера (для шага 2 мастера)
     *
     * null = пользователь ещё не выбрал провайдера
     */
    private val _selectedProviderId = MutableStateFlow<String?>(null)
    val selectedProviderId: StateFlow<String?> = _selectedProviderId.asStateFlow()

    /**
     * Выбрать провайдера
     *
     * Вызывается при клике на карточку провайдера
     */
    fun selectProvider(providerId: String) {
        _selectedProviderId.value = providerId
        println("✅ [AddAccountVM] Выбран провайдер: $providerId")
    }

    /**
     * Получить выбранного провайдера (для отображения на шаге 2)
     *
     * Возвращает Flow, который автоматически обновится если
     * провайдер изменится в БД
     */
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
}
