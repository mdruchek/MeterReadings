// app/src/main/java/ru/dr/meterreadings/screens/AppSettingsScreen.kt
package ru.dr.meterreadings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.dr.meterreadings.models.ui.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    navController: NavController,
    // Текущий выбранный режим темы (приходит из MainActivity)
    appThemeMode: AppThemeMode,
    // Коллбек для смены темы (идёт обратно в MainActivity)
    onThemeChange: (AppThemeMode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 🎨 ГРУППА: ВЫБОР ТЕМЫ (единственная группа настроек)
            SettingsGroup(title = "🎨 Внешний вид") {
                ThemeSettings(
                    currentMode = appThemeMode,
                    onThemeChange = onThemeChange
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Универсальный компонент группы настроек с заголовком и картой.
 * Используется для группировки связанных настроек.
 */
@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Настройки внешнего вида — выбор режима темы приложения.
 */
@Composable
fun ThemeSettings(
    currentMode: AppThemeMode,  // текущий выбранный режим
    onThemeChange: (AppThemeMode) -> Unit  // вызывается при выборе нового режима
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Заголовок группы настроек темы
        Text(
            text = "Тема приложения",
            style = MaterialTheme.typography.titleMedium
        )

        // Вариант 1: Системная тема
        ThemeOptionRow(
            title = "Системная",
            subtitle = "Следует настройкам устройства",
            selected = currentMode == AppThemeMode.SYSTEM,
            onClick = { onThemeChange(AppThemeMode.SYSTEM) }
        )

        // Вариант 2: Светлая тема
        ThemeOptionRow(
            title = "Светлая",
            subtitle = "Всегда светлая тема",
            selected = currentMode == AppThemeMode.LIGHT,
            onClick = { onThemeChange(AppThemeMode.LIGHT) }
        )

        // Вариант 3: Тёмная тема
        ThemeOptionRow(
            title = "Тёмная",
            subtitle = "Всегда тёмная тема",
            selected = currentMode == AppThemeMode.DARK,
            onClick = { onThemeChange(AppThemeMode.DARK) }
        )
    }
}

/**
 * Кликабельная строка с вариантом темы.
 * Показывает название, подзаголовок и RadioButton для выбора.
 */
@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
