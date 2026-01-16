// app/src/main/java/ru/dr/meterreadings/ui/theme/MeterReadingsTheme.kt

package ru.dr.meterreadings.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import ru.dr.meterreadings.models.ui.AppThemeMode

// Стандартная светлая цветовая схема Material 3
private val LightColorScheme = lightColorScheme(
    // Оставляем дефолтные значения Material3.
    // При необходимости позже можно задать свои primary / secondary и т.п.
)

// Стандартная тёмная цветовая схема Material 3
private val DarkColorScheme = darkColorScheme(
    // Аналогично, сейчас используются дефолтные цвета Material3.
)

// Глобальная тема приложения MeterReadings
@Composable
fun MeterReadingsTheme(
    // Режим темы: Светлая / Тёмная / Системная
    themeMode: AppThemeMode,
    // Содержимое, к которому будет применена тема
    content: @Composable () -> Unit
) {
    // Определяем, должна ли сейчас быть активна тёмная тема
    val useDarkTheme = when (themeMode) {
        // Явно светлая тема
        AppThemeMode.LIGHT -> false

        // Явно тёмная тема
        AppThemeMode.DARK -> true

        // Следуем системной настройке (например, системный Dark Mode)
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Выбираем подходящую цветовую схему
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    // Применяем тему ко всему вложенному UI
    MaterialTheme(
        colorScheme = colorScheme,
        // Можно позже вынести Typography/Shapes в отдельные файлы, если потребуется
        content = content
    )
}
