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
import ru.dr.meterreadings.data.util.LogFileManager
import ru.dr.meterreadings.models.domain.AppSettingsDomainModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.models.ui.toUiModel
import ru.dr.meterreadings.workers.WorkerManager
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val providerRepository: ProviderRepository,
    private val workerManager: WorkerManager,
    val logFileManager: LogFileManager
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
            _error.value = null  // Сбрасываем старую ошибку
            try {
                appSettingsRepository.updateGlobalNotifications(enabled)
                println("✅ [ViewModel] Глобальные уведомления: $enabled")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления глобальных уведомлений: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Обновить глобальный флаг уведомлений провайдеров. */
    fun updateProviderNotificationsGlobal(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                appSettingsRepository.updateProviderNotifications(enabled)
                println("✅ [ViewModel] Уведомления провайдеров (глобально): $enabled")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления уведомлений провайдеров: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить уведомления конкретного провайдера.
     * Один флаг управляет и уведомлениями об успехе, и уведомлениями об ошибках.
     */
    fun updateProviderNotifications(providerId: Long, enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                providerRepository.updateProviderNotifications(providerId, enabled)
                println("✅ [ViewModel] Уведомления провайдера $providerId: $enabled")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления уведомлений провайдера: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить время напоминания.
     *
     * @param hour Час (0-23)
     * @param minute Минута (0-59)
     */
    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                appSettingsRepository.updateReminderTime(hour, minute)
                println("✅ [ViewModel] Время напоминания: $hour:$minute")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления времени напоминания: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Включить/выключить глобальные напоминания.
     *
     * @param enabled true = напоминания включены, false = выключены
     */
    fun updateGlobalReminders(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                appSettingsRepository.updateGlobalReminders(enabled)
                println("✅ [ViewModel] Глобальные напоминания: $enabled")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления глобальных напоминаний: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Изменить режим периода напоминаний.
     *
     * @param mode "AUTO" = автоматически по периоду провайдера, "MANUAL" = вручную
     */
    fun updateReminderPeriodMode(mode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                appSettingsRepository.updateReminderPeriodMode(mode)
                println("✅ [ViewModel] Режим периода напоминаний: $mode")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления режима периода: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Изменить количество дней до начала периода передачи показаний,
     * за которое показывается напоминание.
     *
     * @param days Количество дней (обычно 1-7)
     */
    fun updateReminderDaysBeforeStart(days: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                appSettingsRepository.updateReminderDaysBeforeStart(days)
                println("✅ [ViewModel] Дни до начала периода: $days")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления дней до начала периода: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить кастомный день напоминания для конкретного провайдера (режим MANUAL).
     *
     * @param providerId ID провайдера
     * @param day День месяца (1-31)
     */
    fun updateProviderReminderDay(providerId: Long, day: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                providerRepository.updateProviderReminderDay(providerId, day)
                println("✅ [ViewModel] День напоминания провайдера $providerId: $day")
                workerManager.initializeWorkers()
            } catch (e: Exception) {
                val errorMsg = "Ошибка обновления дня напоминания: ${e.message}"
                _error.value = errorMsg
                println("❌ [ViewModel] $errorMsg")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appSettingsRepository.updateLoggingEnabled(enabled)
                logFileManager.setLoggingEnabled(enabled)

                if (enabled) {
                    logFileManager.log("Settings", "📝 Логирование включено")
                }
            } catch (e: Exception) {
                logFileManager.logError("AppSettingsViewModel", "Ошибка обновления настроек логирования", e)
            }
        }
    }

    fun updateLogRetentionDays(days: Int) {
        viewModelScope.launch {
            try {
                appSettingsRepository.updateLogRetentionDays(days)
                logFileManager.log("Settings", "🗑️ Период хранения логов изменён на $days дней")

                // Сразу очищаем старые логи
                if (days > 0) {
                    logFileManager.clearOldLogs(days)
                }
            } catch (e: Exception) {
                logFileManager.logError("AppSettingsViewModel", "Ошибка обновления периода хранения логов", e)
            }
        }
    }

    /**
     * Очистить ошибку (вызывается после показа Snackbar).
     */
    fun clearError() {
        _error.value = null
    }
}
