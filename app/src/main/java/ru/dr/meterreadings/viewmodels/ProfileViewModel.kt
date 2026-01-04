package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.ui.ProfileUiModel
import javax.inject.Inject
import kotlin.Int

/**
 * ViewModel для работы с профилями
 *
 * @HiltViewModel - Hilt автоматически создаст и внедрит зависимости
 * @Inject constructor - Hilt передаст ProfileRepository автоматически
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository  // Hilt передаст автоматически
) : ViewModel() {

    // ========================================
    // STATE - Реактивные данные для UI
    // ========================================

    /**
     * Список всех профилей (с автообновлением из БД)
     *
     * Flow из Repository → StateFlow для UI
     * stateIn - конвертирует Flow в StateFlow:
     * - scope: viewModelScope (отменится при уничтожении ViewModel)
     * - started: WhileSubscribed(5000) - подписка активна пока есть подписчики
     *   + 5 секунд после отписки (оптимизация)
     * - initialValue: emptyList() - начальное значение пока БД не ответила
     */
    val profiles: StateFlow<List<ProfileUiModel>> = repository
        .getAllProfilesFlow()  // Flow<List<ProfileDomainModel>> из Repository
        .map { domainProfiles ->
            // Конвертируем Domain → UI
            domainProfiles.map { domain ->
                ProfileUiModel(
                    profile = domain,
                    addressCount = 0,  // TODO: посчитать из Account
                    accountCount = 0,  // TODO: посчитать из Account
                    readingsCount = 0,
                    lastUpdateDate = null  // TODO: взять из Account
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Текущий выбранный профиль (для ProfileDetailScreen)
     *
     * MutableStateFlow - можно изменять из ViewModel
     * StateFlow - только читать из UI
     */
    private val _profile = MutableStateFlow<ProfileUiModel?>(null)
    val profile: StateFlow<ProfileUiModel?> = _profile.asStateFlow()

    /**
     * Состояние загрузки (для отображения прогресса)
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Ошибки (для отображения SnackBar/Toast)
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ========================================
    // INIT - Инициализация
    // ========================================

    init {
        // При создании ViewModel - создать дефолтный профиль если БД пустая
        viewModelScope.launch {
            val count = repository.getProfileCount()
            println("📊 Профилей в БД: $count")
            if (count == 0) {
                println("✨ Создаём дефолтный профиль")
                createDefaultProfile()
            }
        }
    }

    // ========================================
    // ДЕЙСТВИЯ (ACTIONS)
    // ========================================

    /**
     * Загрузить профиль по ID
     *
     * Вызывается из ProfileDetailScreen
     */
    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val domainProfile = repository.getProfileById(profileId)
                if (domainProfile != null) {
                    _profile.value = ProfileUiModel(
                        profile = domainProfile,
                        addressCount = 0,  // TODO: посчитать
                        accountCount = 0,  // TODO: посчитать
                        readingsCount = 0,
                        lastUpdateDate = null
                    )
                } else {
                    _error.value = "Профиль не найден"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Создать новый профиль
     */
    fun createProfile(name: String, icon: String? = null, isDefault: Boolean = false) {
        viewModelScope.launch {
            try {
                val newProfile = ProfileDomainModel(
                    id = generateProfileId(),
                    name = name,
                    icon = icon,
                    isDefault = isDefault
                )

                // Если это профиль по умолчанию - сбросить флаг у других
                if (isDefault) {
                    repository.setDefaultProfile(newProfile.id)
                } else {
                    repository.saveProfile(newProfile)
                }
            } catch (e: Exception) {
                _error.value = "Ошибка создания профиля: ${e.message}"
            }
        }
    }

    /**
     * Обновить профиль
     */
    fun updateProfile(profile: ProfileDomainModel) {
        viewModelScope.launch {
            try {
                repository.updateProfile(profile)
            } catch (e: Exception) {
                _error.value = "Ошибка обновления: ${e.message}"
            }
        }
    }

    /**
     * Удалить профиль
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProfile(profileId)
            } catch (e: Exception) {
                _error.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    /**
     * Установить профиль по умолчанию
     */
    fun setDefaultProfile(profileId: String) {
        viewModelScope.launch {
            try {
                repository.setDefaultProfile(profileId)
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Очистить ошибку (после показа Toast)
     */
    fun clearError() {
        _error.value = null
    }

    // ========================================
    // ПРИВАТНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Создать дефолтный профиль при первом запуске
     */
    private suspend fun createDefaultProfile() {
        val defaultProfile = ProfileDomainModel(
            id = generateProfileId(),
            name = "Моя недвижимость",
            icon = "🏠",
            isDefault = true
        )
        repository.saveProfile(defaultProfile)
    }

    /**
     * Генерация уникального ID для профиля
     *
     * Использует timestamp + random для уникальности
     */
    private fun generateProfileId(): String {
        return "profile_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}
