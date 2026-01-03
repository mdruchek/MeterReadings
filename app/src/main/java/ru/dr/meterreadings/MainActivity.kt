package ru.dr.meterreadings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.screens.AddAccountWizard
import ru.dr.meterreadings.screens.ProfileDetailScreen
import ru.dr.meterreadings.screens.ProfileListScreen
import ru.dr.meterreadings.viewmodels.ProfileViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    MeterReadingsApp()
                }
            }
        }
    }
}

@Composable
fun MeterReadingsApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "profiles"
    ) {
        // Экран списка профилей
        composable("profiles") {
            ProfileListScreen(navController)
        }

        // Экран деталей профиля
        composable("profile/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            ProfileDetailScreen(
                profileId = profileId,
                navController = navController
            )
        }

        // Мастер добавления аккаунта
        composable("add_account/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""

            // Получаем ViewModel (Hilt создаст автоматически)
            val viewModel: ProfileViewModel = hiltViewModel()

            // Загружаем профиль по ID
            LaunchedEffect(profileId) {
                viewModel.loadProfile(profileId)
            }

            // Подписываемся на изменения профиля
            val profileUi by viewModel.profile.collectAsStateWithLifecycle()

            // Показываем мастер только когда профиль загружен
            profileUi?.let { loadedProfile ->
                AddAccountWizard(
                    profile = loadedProfile.profile,  // Передаем ProfileDomainModel
                    onAccountAdded = { newAccount ->
                        // TODO: Сохранить аккаунт через ViewModel/Repository
                        println("✅ Добавлен аккаунт: ${newAccount.account.accountNumber}")

                        // Просто возвращаемся назад на ProfileDetailScreen
                        navController.popBackStack()
                    },
                    onCancel = {
                        // Callback при отмене
                        navController.navigateUp()
                    }
                )
            } ?: Box(
                // Показываем загрузку пока профиль не загружен
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
