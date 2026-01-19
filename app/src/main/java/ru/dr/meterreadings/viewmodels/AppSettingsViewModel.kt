// app/src/main/java/ru/dr/meterreadings/viewmodels/AppSettingsViewModel.kt
package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dr.meterreadings.models.domain.AppSettingsDomainModel
import ru.dr.meterreadings.data.repository.AppSettingsRepository
import javax.inject.Inject

/**
 * ViewModel для экрана глобальных настроек приложения.
 *
 * Управляет состоянием UI и взаимодействует с Repository.
 * Hilt автоматически создаёт и внедряет зависимости.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val repository: AppSettingsRepository
) : ViewModel() {

    // ========================================
    // СОСТОЯНИЕ UI
    // ========================================

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

    /**
     * Сообщение об ошибке (если есть).
     *
     * null = нет ошибки, String = текст ошибки для показа пользователю.
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ========================================
    // ИНИЦИАЛИЗАЦИЯ
    // ========================================

    init {
        // При создании ViewModel загружаем настройки из БД
        loadSettings()
    }

    /**
     * Загрузить настройки из Repository.
     *
     * Подписывается на Flow из Repository, чтобы получать
     * автоматические обновления при изменениях в БД.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Подписываемся на Flow настроек
                repository.getSettings().collect { settings ->
                    _settings.value = settings
                    println("✅ [AppSettingsViewModel] Настройки загружены: globalNotifications = ${settings.globalNotificationsEnabled}")
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки настроек: ${e.message}"
                println("❌ [AppSettingsViewModel] Ошибка загрузки: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ========================================
    // ДЕЙСТВИЯ (ACTIONS)
    // ========================================

    /**
     * Обновить флаг глобальных уведомлений.
     *
     * Вызывается из UI при переключении Switch.
     * Показывает индикатор загрузки во время сохранения.
     *
     * @param enabled true = включить уведомления, false = выключить
     */
    fun updateGlobalNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Сохраняем в БД через Repository
                repository.updateGlobalNotifications(enabled)

                // Repository автоматически обновит Flow,
                // и настройки обновятся в _settings через collect

                println("✅ [AppSettingsViewModel] Глобальные уведомления обновлены: $enabled")

            } catch (e: Exception) {
                _error.value = "Ошибка сохранения: ${e.message}"
                println("❌ [AppSettingsViewModel] Ошибка сохранения: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить флаг уведомлений провайдеров.
     *
     * Вызывается из UI при переключении Switch.
     *
     * @param enabled true = включить уведомления провайдеров, false = выключить
     */
    fun updateProviderNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Сохраняем в БД через Repository
                repository.updateProviderNotifications(enabled)

                println("✅ [AppSettingsViewModel] Уведомления провайдеров обновлены: $enabled")

            } catch (e: Exception) {
                _error.value = "Ошибка сохранения: ${e.message}"
                println("❌ [AppSettingsViewModel] Ошибка сохранения: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Очистить сообщение об ошибке.
     *
     * Вызывается после показа ошибки пользователю (например, в Snackbar).
     */
    fun clearError() {
        _error.value = null
    }
}
