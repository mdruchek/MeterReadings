package ru.dr.meterreadings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import ru.dr.meterreadings.viewmodels.AddAccountViewModel
import ru.dr.meterreadings.viewmodels.ProfileViewModel

/**
 * Экран добавления аккаунта (аналог Django views.py)
 */
@Composable
fun AddAccountScreen(
    profileId: String,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // Загружаем профиль
    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    val profileUi by viewModel.profile.collectAsStateWithLifecycle()

    profileUi?.let { loadedProfile ->
        AddAccountWizard(
            profile = loadedProfile.profile,
            viewModel = hiltViewModel(),
            onAccountAdded = { newAccount ->
                // ✅ БИЗНЕС-ПРОЦЕСС НА СТРАНИЦЕ (как в Django views)
                viewModel.addAccount(
                    profileId = profileId,
                    providerId = newAccount.account.providerId,
                    accountNumber = newAccount.account.accountNumber
                )

                // ✅ redirect (как в Django)
                navController.popBackStack()
            },
            onCancel = {
                navController.navigateUp()
            }
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()  // ✅ Встроенный компонент Material3
    }
}
