package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
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
    private val providerRepository: ProviderRepository,
    private val repository: ProfileRepository,  // Hilt передаст автоматически
    private val accountRepository: AccountRepository
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
        .getAllProfiles()  // Flow<List<ProfileDomainModel>> из Repository
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
     * Подписывается на изменения профиля (Flow)
     * Профиль автоматически обновится при изменении в БД
     */
    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Подписываемся на Flow профиля
                repository.getProfileById(profileId)
                    .collect { domainProfile ->
                        if (domainProfile != null) {
                            _profile.value = ProfileUiModel(
                                profile = domainProfile,
                                addressCount = 0,  // TODO: repository.getAccountCountForProfile()
                                accountCount = 0,
                                readingsCount = 0,
                                lastUpdateDate = null
                            )
                            _isLoading.value = false
                        } else {
                            _error.value = "Профиль не найден"
                            _isLoading.value = false
                        }
                    }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
                _isLoading.value = false
            }
        }
    }


    /**
     * Создать новый профиль
     */
    fun createProfile(name: String, icon: String? = null) {
        viewModelScope.launch {
            try {
                // repository.createProfile() сам генерирует ID и валидирует
                val newProfileId = repository.createProfile(name, icon)
                //                           ↑ Возвращает ID созданного профиля

                println("✅ Профиль создан с ID: $newProfileId")
            } catch (e: IllegalArgumentException) {
                // Ошибки валидации из Repository
                _error.value = e.message  // "Profile with name 'X' already exists"
            } catch (e: Exception) {
                _error.value = "Ошибка создания профиля: ${e.message}"
            }
        }
    }

    /**
     * Обновить профиль (универсальный метод)
     */
    fun updateProfile(profile: ProfileDomainModel) {
        viewModelScope.launch {
            try {
                repository.updateProfile(profile)
            } catch (e: IllegalArgumentException) {
                _error.value = e.message  // "Profile not found"
            } catch (e: Exception) {
                _error.value = "Ошибка обновления: ${e.message}"
            }
        }
    }

    /**
     * Обновить только имя профиля (с валидацией)
     */
    fun updateProfileName(profileId: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.updateProfileName(profileId, newName)
                println("✅ Профиль переименован в '$newName'")

            } catch (e: IllegalArgumentException) {
                // Ошибки валидации:
                // - "Profile not found"
                // - "Profile with name 'X' already exists"
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = "Ошибка переименования: ${e.message}"
            }
        }
    }

    /**
     * Обновить иконку профиля
     */
    fun updateProfileIcon(profileId: String, newIcon: String) {
        viewModelScope.launch {
            try {
                val domainProfile = repository.getProfileById(profileId).first()
                if (domainProfile != null) {
                    repository.updateProfile(domainProfile.copy(icon = newIcon))
                    println("✅ Иконка обновлена")
                } else {
                    _error.value = "Профиль не найден"
                }
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = "Ошибка обновления иконки: ${e.message}"
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
     * Добавить аккаунт к профилю
     */
    fun addAccount(
        profileId: String,
        providerId: String,
        accountNumber: String
    ) {
        viewModelScope.launch {
            try {
                println("💾 [ProfileViewModel] Добавляем аккаунт: $accountNumber")

                val accountId = accountRepository.addAccount(
                    profileId = profileId,
                    providerId = providerId,
                    accountNumber = accountNumber
                )

                println("✅ [ProfileViewModel] Аккаунт добавлен с ID: $accountId")

            } catch (e: Exception) {
                println("❌ [ProfileViewModel] Ошибка добавления аккаунта: ${e.message}")
                e.printStackTrace()
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
        try {
            val profileId = repository.createProfile("Моя недвижимость")
            //                        ↑ Новый метод (генерирует ID сам)
            println("✅ Создан дефолтный профиль с ID: $profileId")

        } catch (e: IllegalArgumentException) {
            // Профиль с таким именем уже существует (не должно произойти)
            println("⚠️ Дефолтный профиль уже существует: ${e.message}")
        } catch (e: Exception) {
            println("❌ Ошибка создания дефолтного профиля: ${e.message}")
            _error.value = "Не удалось создать профиль"
        }
    }

    /**
     * 🧪 ТЕСТОВЫЙ метод для проверки загрузки конфигураций БД региона
     */
    fun testLoadKvcLocations() {
        viewModelScope.launch {
            println("🧪 [ViewModel] ТЕСТ: Загружаем конфигурации БД для региона 30...")

            val result = providerRepository.getKvcLocationsForRegion(regionId = 30)

            result.onSuccess { locations ->
                println("✅ [ViewModel] Успех! Получено конфигураций: ${locations.size}")
                locations.forEach { location ->
                    println("   💾 Server: ${location.server}, DB: ${location.dbName}, User: ${location.idUser}")
                }
            }

            result.onFailure { error ->
                println("❌ [ViewModel] Ошибка: ${error.message}")
                error.printStackTrace()
            }
        }
    }
}
