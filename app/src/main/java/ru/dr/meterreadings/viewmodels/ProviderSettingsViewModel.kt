// app/src/main/java/ru/dr/meterreadings/viewmodels/ProviderSettingsViewModel.kt

package ru.dr.meterreadings.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.workers.MeterReadingNotificationWorker
import ru.dr.meterreadings.workers.PeriodUpdateWorker
import javax.inject.Inject

/**
 * ViewModel для экрана настроек провайдера
 */
@HiltViewModel
class ProviderSettingsViewModel @Inject constructor(
    private val app: Application,
    private val profileRepository: ProfileRepository
) : AndroidViewModel(app) {

    // ============================================
    // STATE
    // ============================================

    private val _provider = MutableStateFlow<ProviderDomainModel?>(null)
    val provider: StateFlow<ProviderDomainModel?> = _provider.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // ============================================
    // PUBLIC METHODS
    // ============================================

    /**
     * Загрузить настройки провайдера
     */
    fun loadProvider(providerId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            profileRepository.getProviderById(providerId).collect { provider ->
                _provider.value = provider
                _isLoading.value = false

                println("✅ [ProviderSettingsVM] Провайдер загружен: ${provider?.name}")
            }
        }
    }

    /**
     * Обновить настройки напоминаний
     */
    fun updateReminderSettings(
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        val current = _provider.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                profileRepository.updateProviderSettings(
                    providerId = current.id,
                    autoUpdateEnabled = current.autoUpdateEnabled,
                    updateStartDay = current.updateStartDay,
                    updateIntervalHours = current.updateIntervalHours,
                    updateNotificationsEnabled = current.updateNotificationsEnabled,
                    errorNotificationsEnabled = current.errorNotificationsEnabled,
                    reminderEnabled = enabled,
                    reminderTimeHour = hour,
                    reminderTimeMinute = minute,
                    reminderPeriodMode = current.reminderPeriodMode,
                    reminderCustomStartDay = current.reminderCustomStartDay,
                    reminderCustomEndDay = current.reminderCustomEndDay
                )

                // Перезапуск Worker напоминаний
                if (enabled) {
                    MeterReadingNotificationWorker.schedule(
                        context = app.applicationContext,
                        hour = hour,
                        minute = minute
                    )
                } else {
                    MeterReadingNotificationWorker.cancel(app.applicationContext)
                }

                _saveSuccess.value = true
                println("✅ [ProviderSettingsVM] Напоминания: $enabled в $hour:$minute")

            } catch (e: Exception) {
                _error.value = "Не удалось сохранить настройки"
                println("❌ [ProviderSettingsVM] Ошибка: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить режим периода напоминаний
     */
    fun updateReminderPeriodMode(
        mode: String,
        customStartDay: Int? = null,
        customEndDay: Int? = null
    ) {
        val current = _provider.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                profileRepository.updateProviderSettings(
                    providerId = current.id,
                    autoUpdateEnabled = current.autoUpdateEnabled,
                    updateStartDay = current.updateStartDay,
                    updateIntervalHours = current.updateIntervalHours,
                    updateNotificationsEnabled = current.updateNotificationsEnabled,
                    errorNotificationsEnabled = current.errorNotificationsEnabled,
                    reminderEnabled = current.reminderEnabled,
                    reminderTimeHour = current.reminderTimeHour,
                    reminderTimeMinute = current.reminderTimeMinute,
                    reminderPeriodMode = mode,
                    reminderCustomStartDay = customStartDay,
                    reminderCustomEndDay = customEndDay
                )

                _saveSuccess.value = true
                println("✅ [ProviderSettingsVM] Режим: $mode ($customStartDay-$customEndDay)")

            } catch (e: Exception) {
                _error.value = "Не удалось сохранить настройки"
                println("❌ [ProviderSettingsVM] Ошибка: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить настройки автообновления
     */
    fun updateAutoUpdateSettings(
        enabled: Boolean,
        intervalHours: Int
    ) {
        val current = _provider.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                profileRepository.updateProviderSettings(
                    providerId = current.id,
                    autoUpdateEnabled = enabled,
                    updateStartDay = current.updateStartDay,
                    updateIntervalHours = intervalHours,
                    updateNotificationsEnabled = current.updateNotificationsEnabled,
                    errorNotificationsEnabled = current.errorNotificationsEnabled,
                    reminderEnabled = current.reminderEnabled,
                    reminderTimeHour = current.reminderTimeHour,
                    reminderTimeMinute = current.reminderTimeMinute,
                    reminderPeriodMode = current.reminderPeriodMode,
                    reminderCustomStartDay = current.reminderCustomStartDay,
                    reminderCustomEndDay = current.reminderCustomEndDay
                )

                // Перезапуск Worker автообновления
                if (enabled) {
                    PeriodUpdateWorker.schedule(
                        context = app.applicationContext,
                        intervalHours = intervalHours
                    )
                } else {
                    PeriodUpdateWorker.cancel(app.applicationContext)
                }

                _saveSuccess.value = true
                println("✅ [ProviderSettingsVM] Автообновление: $enabled ($intervalHours ч)")

            } catch (e: Exception) {
                _error.value = "Не удалось сохранить настройки"
                println("❌ [ProviderSettingsVM] Ошибка: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить настройки уведомлений
     */
    fun updateNotificationSettings(
        updateEnabled: Boolean,
        errorEnabled: Boolean
    ) {
        val current = _provider.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                profileRepository.updateProviderSettings(
                    providerId = current.id,
                    autoUpdateEnabled = current.autoUpdateEnabled,
                    updateStartDay = current.updateStartDay,
                    updateIntervalHours = current.updateIntervalHours,
                    updateNotificationsEnabled = updateEnabled,
                    errorNotificationsEnabled = errorEnabled,
                    reminderEnabled = current.reminderEnabled,
                    reminderTimeHour = current.reminderTimeHour,
                    reminderTimeMinute = current.reminderTimeMinute,
                    reminderPeriodMode = current.reminderPeriodMode,
                    reminderCustomStartDay = current.reminderCustomStartDay,
                    reminderCustomEndDay = current.reminderCustomEndDay
                )

                _saveSuccess.value = true
                println("✅ [ProviderSettingsVM] Уведомления: передача=$updateEnabled, ошибки=$errorEnabled")

            } catch (e: Exception) {
                _error.value = "Не удалось сохранить настройки"
                println("❌ [ProviderSettingsVM] Ошибка: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Очистить флаг успешного сохранения
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    /**
     * Очистить ошибку
     */
    fun clearError() {
        _error.value = null
    }
}
