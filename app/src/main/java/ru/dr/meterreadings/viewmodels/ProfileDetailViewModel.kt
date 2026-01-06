package ru.dr.meterreadings.viewmodels  // ← Изменили package!

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileId: String = checkNotNull(savedStateHandle["profileId"]) {
        "profileId is required"
    }

    private val _profile = MutableStateFlow<ProfileDomainModel?>(null)
    val profile: StateFlow<ProfileDomainModel?> = _profile.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountDomainModel>>(emptyList())
    val accounts: StateFlow<List<AccountDomainModel>> = _accounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        println("🔍 [ViewModel] ProfileDetailViewModel создан для профиля: $profileId")
        loadProfileData()
    }

    private fun loadProfileData() {
        // =====================================================
        // КОРУТИНА 1: Загрузка профиля (один раз)
        // =====================================================
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                println("📥 [ViewModel] Загружаем профиль: $profileId")

                val profile = profileRepository.getProfileById(profileId).first()
                if (profile == null) {
                    _error.value = "Профиль не найден"
                    println("❌ [ViewModel] Профиль $profileId не найден")
                    _isLoading.value = false
                    return@launch
                }

                _profile.value = profile
                println("✅ [ViewModel] Профиль загружен: ${profile.name}")
                _isLoading.value = false

            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
                println("❌ [ViewModel] Ошибка загрузки профиля: ${e.message}")
                e.printStackTrace()
                _isLoading.value = false
            }
        }

        // =====================================================
        // КОРУТИНА 2: Слушаем аккаунты (постоянно)
        // =====================================================
        viewModelScope.launch {
            try {
                accountRepository.getAccountsByProfileId(profileId).collect { accounts ->
                    println("🔔 [ViewModel] Flow обновился! Счетов: ${accounts.size}")
                    _accounts.value = accounts
                    accounts.forEach { account ->
                        println("  💳 [ViewModel] ${account.accountNumber} (${account.providerId})")
                    }
                }
            } catch (e: CancellationException) {
                // Игнорируем отмену корутины при уничтожении ViewModel
                println("⏹️ [ViewModel] Корутина слушания аккаунтов отменена (это нормально)")
            } catch (e: Exception) {
                println("❌ [ViewModel] Ошибка слушания аккаунтов: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                println("🗑️ [ViewModel] Удаляем профиль: $profileId")

                if (_profile.value == null) {
                    _error.value = "Профиль не найден"
                    return@launch
                }

                profileRepository.deleteProfile(profileId)
                println("✅ [ViewModel] Профиль удалён!")
                onSuccess()

            } catch (e: Exception) {
                _error.value = "Ошибка удаления: ${e.message}"
                println("❌ [ViewModel] Ошибка удаления профиля: ${e.message}")
            }
        }
    }

    /**
     * Удалить лицевой счёт
     *
     * Вызывает AccountRepository для удаления из БД.
     * Благодаря Flow, UI автоматически обновится после удаления.
     *
     * @param accountId ID счёта для удаления
     */
    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                println("🗑️ [ViewModel] Удаляем аккаунт: $accountId")

                // Вызываем Repository для удаления из БД
                accountRepository.deleteAccount(accountId)

                println("✅ [ViewModel] Аккаунт удалён!")

                // UI автоматически обновится благодаря Flow в loadProfileData()
                // Нам не нужно вручную обновлять _accounts - Flow сделает это сам!

            } catch (e: Exception) {
                // Обрабатываем ошибки (например, счёт не найден)
                _error.value = "Ошибка удаления: ${e.message}"
                println("❌ [ViewModel] Ошибка удаления аккаунта: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

