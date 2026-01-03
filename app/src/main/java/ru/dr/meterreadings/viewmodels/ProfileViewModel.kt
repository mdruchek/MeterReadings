package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.dr.meterreadings.models.ui.ProfileUiModel // ← Импорт UI модели
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    // private val repository: ProfileRepository - добавим позже
) : ViewModel() {

    // Храним UI модель (не Domain!)
    private val _profile = MutableStateFlow<ProfileUiModel?>(null)
    val profile: StateFlow<ProfileUiModel?> = _profile

    /**
     * Загружает профиль по ID и преобразует в UI модель
     */
    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            // TODO: Получить из репозитория
            // val domainModel = repository.getProfile(profileId)

            // Моковые данные (временно)
            val domainModel = ProfileDomainModel(
                id = profileId,
                name = "Моя недвижимость",
                icon = "🏠",
                isDefault = true
            )

            // Преобразуем Domain → UI
            val uiModel = ProfileUiModel(
                profile = domainModel,
                accountCount = 3,    // TODO: Посчитать из БД
                addressCount = 2,    // TODO: Посчитать из БД
                readingsCount = 12,  // TODO: Посчитать из БД
                lastUpdateDate = "03.01.2026" // TODO: Форматировать реальную дату
            )

            _profile.value = uiModel
        }
    }
}
