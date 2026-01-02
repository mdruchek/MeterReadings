package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _profile = MutableStateFlow<ProfileDomainModel?>(null)
    val profile: StateFlow<ProfileDomainModel?> = _profile.asStateFlow()

    fun loadProfile(profileId: String) {
        viewModelScope.launch {
            // TODO: реальная загрузка из репозитория
            _profile.value = ProfileDomainModel(
                id = profileId,
                name = "Профиль #$profileId",
                addressesCount = 3,
                companiesCount = 5,
                readingsCount = 12
            )
        }
    }
}
