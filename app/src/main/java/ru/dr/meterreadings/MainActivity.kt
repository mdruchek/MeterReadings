// app/src/main/java/ru/dr/meterreadings/MainActivity.kt

package ru.dr.meterreadings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.dr.meterreadings.screens.AddAccountScreen
import ru.dr.meterreadings.screens.AppSettingsScreen
import ru.dr.meterreadings.screens.ProfileDetailScreen
import ru.dr.meterreadings.screens.ProfileListScreen
import ru.dr.meterreadings.screens.ProviderSettingsScreen  // ✨ НОВОЕ

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Launcher для запроса разрешения на уведомления
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ [MainActivity] Разрешение на уведомления получено")
        } else {
            println("⚠️ [MainActivity] Разрешение на уведомления отклонено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запрашиваем разрешение на уведомления (Android 13+)
        requestNotificationPermission()

        setContent {
            MaterialTheme {
                Surface {
                    MeterReadingsApp()
                }
            }
        }
    }

    // Запросить разрешение на уведомления (Android 13+)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            when {
                ContextCompat.checkSelfPermission(this, permission) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    println("✅ [MainActivity] Разрешение на уведомления уже есть")
                }
                else -> {
                    println("🔔 [MainActivity] Запрашиваем разрешение на уведомления")
                    notificationPermissionLauncher.launch(permission)
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
            AddAccountScreen(
                profileId = profileId,
                navController = navController
            )
        }

        // ✨ НОВОЕ: Экран настроек провайдера
        composable("provider_settings/{providerId}") { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            ProviderSettingsScreen(
                providerId = providerId,
                navController = navController
            )
        }


        composable("settings") {
            AppSettingsScreen(navController)
        }
    }
}
