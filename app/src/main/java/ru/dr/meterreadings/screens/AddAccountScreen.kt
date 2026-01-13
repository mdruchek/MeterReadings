package ru.dr.meterreadings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dr.meterreadings.viewmodels.ProfileViewModel

@Composable
fun AddAccountScreen(
    profileId: String,
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // ✅ ИСПРАВЛЕНО: collectAsStateWithLifecycle() без initial
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    // ✅ Безопасный доступ
    val profile = remember(profiles, profileId) {
        profiles.find { it.profile.id == profileId }?.profile
    }

    when {
        profiles.isEmpty() -> {
            // Показываем загрузку пока профили не загрузились
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        profile == null -> {
            // Профиль не найден
            LaunchedEffect(Unit) {
                navController.navigateUp()
            }
        }
        else -> {
            // Показываем wizard
            AddAccountWizard(
                profile = profile,
                onCancel = {
                    navController.navigateUp()
                }
            )
        }
    }
}
