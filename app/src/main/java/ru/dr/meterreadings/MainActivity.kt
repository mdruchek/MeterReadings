package ru.dr.meterreadings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.dr.meterreadings.screens.ProfileDetailScreen
import ru.dr.meterreadings.screens.ProfileListScreen

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
        composable("profiles") {
            ProfileListScreen(navController)
        }

        // УПРОЩЕННАЯ версия БЕЗ NavType
        composable("profile/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            ProfileDetailScreen(
                profileId = profileId,
                navController = navController
            )
        }
    }
}
