// app/src/main/java/ru/dr/meterreadings/screens/AppSettingsScreen.kt
package ru.dr.meterreadings.screens

import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.dr.meterreadings.models.ui.AppThemeMode
import ru.dr.meterreadings.viewmodels.AppSettingsViewModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    navController: NavController,
    // Текущий выбранный режим темы (приходит из MainActivity)
    appThemeMode: AppThemeMode,
    // Коллбек для смены темы (идёт обратно в MainActivity)
    onThemeChange: (AppThemeMode) -> Unit,
    // ViewModel для глобальных настроек (автоматически создаётся Hilt)
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    // Собираем состояние из ViewModel
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val providers by viewModel.providers.collectAsState()

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

            // 🔔 ГРУППА: УВЕДОМЛЕНИЯ
            SettingsGroup(title = "🔔 Уведомления") {
                // Если настройки ещё загружаются, показываем индикатор
                if (settings == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // Глобальный переключатель уведомлений
                    GlobalNotificationSwitch(
                        enabled = settings!!.globalNotificationsEnabled,
                        isLoading = isLoading,
                        onCheckedChange = { enabled ->
                            viewModel.updateGlobalNotifications(enabled)
                        }
                    )

                    // Показываем настройки провайдеров только если глобальные уведомления включены
                    if (settings!!.globalNotificationsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Переключатель уведомлений провайдеров
                        ProviderNotificationSwitch(
                            enabled = settings!!.providerNotificationsEnabled,
                            isLoading = isLoading,
                            onCheckedChange = { enabled ->
                                viewModel.updateProviderNotificationsGlobal(enabled)
                            }
                        )

                        // Показываем список провайдеров только если их уведомления включены
                        if (settings!!.providerNotificationsEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider()

                            Spacer(modifier = Modifier.height(16.dp))

                            // Список провайдеров с индивидуальными настройками
                            ProvidersNotificationsList(
                                providers = providers,  // List<ProviderUiModel>
                                isLoading = isLoading,
                                onProviderNotificationChange = { providerId, enabled ->  // ← ОБНОВЛЕНО
                                    viewModel.updateProviderNotifications(providerId, enabled)
                                }
                            )
                        }
                    }
                }
            }
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
    // Локальное состояние: развернута ли группа
    // По умолчанию false = группы свернуты при открытии экрана
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // Заголовок группы с иконкой стрелки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded  // Меняем состояние при клике
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Название группы
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            // Иконка стрелки: поворачивается в зависимости от состояния
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowDown  // ↓ вниз когда развернута
                } else {
                    Icons.Default.KeyboardArrowRight  // → вправо когда свернута
                },
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(16.dp)) {
                    content()
                }
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

/**
 * Глобальный переключатель всех уведомлений приложения.
 *
 * Мастер-флаг: если выключен, то все уведомления отключены,
 * независимо от настроек отдельных провайдеров.
 */
@Composable
private fun GlobalNotificationSwitch(
    enabled: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Иконка и текст слева
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Все уведомления",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (enabled) {
                        "Уведомления включены для всего приложения"
                    } else {
                        "Уведомления отключены полностью"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Switch справа
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            enabled = !isLoading  // Отключаем во время сохранения
        )
    }
}

/**
 * Глобальный переключатель уведомлений для всех провайдеров.
 *
 * Включает/выключает уведомления провайдеров независимо от их индивидуальных настроек.
 * Показывается только если глобальные уведомления включены.
 */
@Composable
private fun ProviderNotificationSwitch(
    enabled: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Уведомления провайдеров",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (enabled) {
                        "Уведомления для всех провайдеров включены"
                    } else {
                        "Уведомления провайдеров отключены"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            enabled = !isLoading
        )
    }
}

/**
 * Список провайдеров с переключателями уведомлений.
 *
 * Каждый провайдер в одну строку: логотип, название, переключатель.
 */
@Composable
private fun ProvidersNotificationsList(
    providers: List<ProviderUiModel>,  // ← UI модель
    isLoading: Boolean,
    onProviderNotificationChange: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Уведомления для каждого поставщика",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (providers.isEmpty()) {
            Text(
                text = "Поставщики не добавлены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            providers.forEach { providerUi ->
                ProviderNotificationRow(
                    providerUi = providerUi,
                    isLoading = isLoading,
                    onNotificationChange = onProviderNotificationChange
                )
            }
        }
    }
}

/**
 * Строка с одним провайдером: логотип | название | переключатель
 */
@Composable
private fun ProviderNotificationRow(
    providerUi: ProviderUiModel,  // ← UI модель
    isLoading: Boolean,
    onNotificationChange: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Логотип провайдера (кешируется через Coil)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(providerUi.provider.logoUrl)  // ← Доступ через .provider
                .crossfade(true)
                .placeholder(R.drawable.ic_provider_placeholder)
                .error(R.drawable.ic_provider_placeholder)
                .build(),
            contentDescription = "Логотип ${providerUi.provider.name}",
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Fit
        )

        // Название провайдера
        Text(
            text = providerUi.provider.name,  // ← Доступ через .provider
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Переключатель
        Switch(
            checked = providerUi.provider.notificationsEnabled,  // ← Доступ через .provider
            onCheckedChange = { enabled ->
                onNotificationChange(providerUi.provider.id, enabled)  // ← Доступ через .provider
            },
            enabled = !isLoading && !providerUi.isLoading,  // ← UI состояние
            modifier = Modifier.scale(0.85f)
        )
    }
}
