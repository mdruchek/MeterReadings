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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.dr.meterreadings.models.ui.AppThemeMode
import ru.dr.meterreadings.screens.AddAccountScreen
import ru.dr.meterreadings.screens.ProfileDetailScreen
import ru.dr.meterreadings.screens.ProfileListScreen
import ru.dr.meterreadings.screens.AppSettingsScreen
import ru.dr.meterreadings.ui.theme.MeterReadingsTheme   // новый MeterReadingsTheme с AppThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Лаунчер для запроса разрешения на отправку уведомлений (Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                println("MainActivity: уведомления разрешены")
            } else {
                println("MainActivity: уведомления НЕ разрешены")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // При старте Activity запрашиваем разрешение на уведомления (если нужно)
        requestNotificationPermission()

        setContent {
            // Текущее состояние режима темы приложения.
            // rememberSaveable сохранит значение при пересоздании Activity/повороте экрана.
            var appThemeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }

            // Оборачиваем всё приложение в тему MeterReadingsTheme
            MeterReadingsTheme(themeMode = appThemeMode) {
                // Базовая поверхность с фоном из colorScheme.background
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Основной composable приложения с навигацией
                    MeterReadingsApp(
                        appThemeMode = appThemeMode,
                        onThemeChange = { newMode ->
                            appThemeMode = newMode
                        }
                    )
                }
            }
        }
    }

    /**
     * Запрос разрешения на показ уведомлений для Android 13+ (TIRAMISU).
     * Для более старых версий Android ничего не делаем.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            when (ContextCompat.checkSelfPermission(this, permission)) {
                PackageManager.PERMISSION_GRANTED -> {
                    println("MainActivity: permission already granted")
                }
                else -> {
                    println("MainActivity: requesting notification permission")
                    notificationPermissionLauncher.launch(permission)
                }
            }
        }
    }
}

/**
 * Главный composable приложения:
 * - создаёт NavController;
 * - описывает граф навигации;
 * - прокидывает настройки темы на экран настроек.
 */
@Composable
fun MeterReadingsApp(
    // Текущий выбранный режим темы
    appThemeMode: AppThemeMode,
    // Коллбек, который будет вызываться при смене темы на экране настроек
    onThemeChange: (AppThemeMode) -> Unit
) {
    // Контроллер навигации между экранами
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "profiles" // стартовый экран — список профилей
    ) {
        // Экран со списком профилей
        composable("profiles") {
            ProfileListScreen(navController = navController)
        }

        // Экран детализации профиля
        composable("profile/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            ProfileDetailScreen(
                profileId = profileId,
                navController = navController
            )
        }

        // Экран добавления счёта (если он у тебя есть)
        composable("add_account/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            AddAccountScreen(
                profileId = profileId,
                navController = navController
            )
        }

        // Экран настроек приложения (в том числе выбора темы)
        composable("settings") {
            AppSettingsScreen(
                navController = navController,
                appThemeMode = appThemeMode,
                onThemeChange = onThemeChange
            )
        }
    }
}
