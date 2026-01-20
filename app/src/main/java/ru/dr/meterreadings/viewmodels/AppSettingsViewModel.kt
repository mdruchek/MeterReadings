// app/src/main/java/ru/dr/meterreadings/viewmodels/AppSettingsViewModel.kt
package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.AppSettingsRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.models.domain.AppSettingsDomainModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.models.ui.toUiModel
import javax.inject.Inject

/**
 * ViewModel для экрана глобальных настроек приложения.
 *
 * Управляет состоянием UI и взаимодействует с Repository.
 * Hilt автоматически создаёт и внедряет зависимости.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    // ============================================
    // STATE (UI наблюдает за этими StateFlow)
    // ============================================

    /**
     * Текущие настройки приложения.
     *
     * StateFlow автоматически обновляет UI при изменениях в БД.
     * Инициализируется null, потом загружается из Repository.
     */
    private val _settings = MutableStateFlow<AppSettingsDomainModel?>(null)
    val settings: StateFlow<AppSettingsDomainModel?> = _settings.asStateFlow()

    /**
     * Флаг загрузки данных.
     *
     * true = идёт загрузка/сохранение, показываем индикатор прогресса.
     * false = операция завершена, UI активен.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Список провайдеров (UI модели)
    private val _providers = MutableStateFlow<List<ProviderUiModel>>(emptyList())
    val providers: StateFlow<List<ProviderUiModel>> = _providers.asStateFlow()

    /**
     * Сообщение об ошибке (если есть).
     *
     * null = нет ошибки, String = текст ошибки для показа пользователю.
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        // Загружаем глобальные настройки
        viewModelScope.launch {
            appSettingsRepository.getSettings()
                .collect { settings ->
                    _settings.value = settings
                    println("✅ [ViewModel] Настройки загружены: $settings")
                }
        }

        // Загружаем провайдеров и конвертируем в UI модели
        viewModelScope.launch {
            providerRepository.getAllProviders()
                .map { domainList ->
                    domainList.map { it.toUiModel() } // Domain → UiModel
                }
                .collect { uiList ->
                    _providers.value = uiList
                    println("✅ [ViewModel] Провайдеры загружены: ${uiList.size} шт.")
                }
        }
    }

    // ============================================
    // ACTIONS (UI вызывает эти методы)
    // ============================================

    /** Обновить глобальные уведомления (мастер-флаг). */
    fun updateGlobalNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appSettingsRepository.updateGlobalNotifications(enabled)
                println("✅ [ViewModel] Глобальные уведомления: $enabled")
            } catch (e: Exception) {
                println("❌ [ViewModel] Ошибка обновления глобальных уведомлений: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Обновить уведомления провайдеров (общий флаг). */
    fun updateProviderNotificationsGlobal(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appSettingsRepository.updateProviderNotifications(enabled)
                println("✅ [ViewModel] Уведомления провайдеров: $enabled")
            } catch (e: Exception) {
                println("❌ [ViewModel] Ошибка обновления уведомлений провайдеров: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить уведомления конкретного провайдера.
     *
     * @param providerId ID провайдера
     * @param enabled Включены ли уведомления
     */
    fun updateProviderNotifications(providerId: String, enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                providerRepository.updateProviderNotifications(providerId, enabled)
                println("✅ [ViewModel] Уведомления провайдера $providerId: $enabled")
            } catch (e: Exception) {
                println("❌ [ViewModel] Ошибка обновления уведомлений провайдера: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
