// app/src/main/java/ru/dr/meterreadings/screens/AppSettingsScreen.kt
package ru.dr.meterreadings.screens

import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight  // ✅ ДОБАВЬ ЭТУ СТРОКУ
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.dr.meterreadings.models.ui.AppThemeMode
import ru.dr.meterreadings.viewmodels.AppSettingsViewModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.ui.components.DayPickerDialog
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
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val error by viewModel.error.collectAsState()

    // SnackBar для ошибок
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()  // Сбрасываем после показа
        }
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            Spacer(modifier = Modifier.height(32.dp))

            // ⏰ ГРУППА: НАПОМИНАНИЯ (показывается только если globalNotificationsEnabled = true)
            if (settings?.globalNotificationsEnabled == true) {
                Spacer(modifier = Modifier.height(32.dp))

                SettingsGroup(title = "⏰ Напоминания") {
                    if (settings == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        // Глобальный переключатель напоминаний
                        GlobalRemindersSwitch(
                            enabled = settings!!.globalRemindersEnabled,
                            isLoading = isLoading,
                            onCheckedChange = { enabled ->
                                viewModel.updateGlobalReminders(enabled)
                            }
                        )

                        // Показываем настройки только если напоминания включены
                        if (settings!!.globalRemindersEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // ⏰ Время напоминания (глобально)
                            ReminderTimeSetting(
                                hour = settings!!.reminderTimeHour,
                                minute = settings!!.reminderTimeMinute,
                                isLoading = isLoading,
                                onTimeChange = { hour, minute ->
                                    viewModel.updateReminderTime(hour, minute)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            // 📅 Режим периода (AUTO/MANUAL)
                            ReminderPeriodModeSelector(
                                mode = settings!!.reminderPeriodMode,
                                isLoading = isLoading,
                                onModeChange = { mode ->
                                    viewModel.updateReminderPeriodMode(mode)
                                }
                            )

                            // Если режим AUTO, показываем настройку дней
                            if (settings!!.reminderPeriodMode == "AUTO") {
                                Spacer(modifier = Modifier.height(16.dp))
                                ReminderDaysBeforeSlider(
                                    days = settings!!.reminderDaysBeforeStart,
                                    isLoading = isLoading,
                                    onDaysChange = { days ->
                                        viewModel.updateReminderDaysBeforeStart(days)
                                    }
                                )

                                // Показываем список провайдеров с рассчитанными днями (только просмотр)
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                ProvidersRemindersAutoList(
                                    providers = providers,
                                    daysBeforeStart = settings!!.reminderDaysBeforeStart
                                )
                            }

                            // Если режим MANUAL, показываем список провайдеров
                            if (settings!!.reminderPeriodMode == "MANUAL") {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                ProvidersRemindersList(
                                    providers = providers,
                                    isLoading = isLoading,
                                    onProviderReminderDayChange = { providerId, day ->
                                        viewModel.updateProviderReminderDay(providerId, day)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ✅ НОВОЕ: Показываем подсказку, если глобальные уведомления отключены
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )

                        Column {
                            Text(
                                text = "Напоминания недоступны",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Включите глобальные уведомления, чтобы настроить напоминания о передаче показаний",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ✅ ДОБАВЛЯЕМ СЕКЦИЮ "ЛОГИ"
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Заголовок
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 Логи приложения",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Switch(
                            checked = settings?.loggingEnabled ?: true,
                            onCheckedChange = { viewModel.updateLoggingEnabled(it) }
                        )
                    }

                    if (settings?.loggingEnabled == true) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Информация о логах
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Размер: ${viewModel.logFileManager.getLogFileSize()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Записей: ${viewModel.logFileManager.getLogLineCount()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Период автоматической очистки
                        Text(
                            text = "Автоматическая очистка",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Выбор периода
                        var showRetentionDialog by remember { mutableStateOf(false) }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRetentionDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Хранить логи",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = when (settings?.logRetentionDays ?: 7) {
                                            0 -> "Бессрочно"
                                            1 -> "1 день"
                                            else -> "${settings?.logRetentionDays ?: 7} дней"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Диалог выбора периода
                        if (showRetentionDialog) {
                            AlertDialog(
                                onDismissRequest = { showRetentionDialog = false },
                                title = { Text("Период хранения логов") },
                                text = {
                                    Column {
                                        val options = listOf(
                                            0 to "Бессрочно (не удалять)",
                                            1 to "1 день",
                                            3 to "3 дня",
                                            7 to "7 дней (неделя)",
                                            14 to "14 дней (2 недели)",
                                            30 to "30 дней (месяц)"
                                        )

                                        options.forEach { (days, label) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.updateLogRetentionDays(days)
                                                        showRetentionDialog = false
                                                    }
                                                    .padding(vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = settings?.logRetentionDays == days,
                                                    onClick = {
                                                        viewModel.updateLogRetentionDays(days)
                                                        showRetentionDialog = false
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(label)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showRetentionDialog = false }) {
                                        Text("Закрыть")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Кнопки управления
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Кнопка "Отправить"
                            Button(
                                onClick = {
                                    viewModel.logFileManager.shareLogFile(context as Activity)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Отправить")
                            }

                            // Кнопка "Очистить"
                            OutlinedButton(
                                onClick = {
                                    viewModel.logFileManager.clearLogs()
                                    Toast.makeText(context, "Логи очищены", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Очистить")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Логирование отключено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
    onProviderNotificationChange: (Long, Boolean) -> Unit
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
    onNotificationChange: (Long, Boolean) -> Unit
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

/**
 * Глобальный переключатель напоминаний.
 *
 * Мастер-флаг: если выключен, то напоминания для всех провайдеров отключены.
 */
@Composable
private fun GlobalRemindersSwitch(
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
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Напоминания о передаче показаний",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (enabled) {
                        "Напоминания включены"
                    } else {
                        "Напоминания отключены"
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
 * Настройка времени напоминания (глобально для всех провайдеров).
 */
@Composable
private fun ReminderTimeSetting(
    hour: Int,
    minute: Int,
    isLoading: Boolean,
    onTimeChange: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { showTimePicker = true }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Время напоминания",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Когда показывать напоминания",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = String.format("%02d:%02d", hour, minute),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    // TimePicker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { selectedHour, selectedMinute ->
                onTimeChange(selectedHour, selectedMinute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/**
 * Выбор режима периода: AUTO (по периоду провайдера) или MANUAL (вручную).
 */
@Composable
private fun ReminderPeriodModeSelector(
    mode: String,
    isLoading: Boolean,
    onModeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Режим расчёта периода",
            style = MaterialTheme.typography.titleMedium
        )

        // Вариант 1: Автоматически
        ReminderModeRow(
            title = "Автоматически",
            subtitle = "По периоду передачи с сайта провайдера",
            selected = mode == "AUTO",
            enabled = !isLoading,
            onClick = { onModeChange("AUTO") }
        )

        // Вариант 2: Вручную
        ReminderModeRow(
            title = "Вручную",
            subtitle = "Указать день месяца для каждого провайдера",
            selected = mode == "MANUAL",
            enabled = !isLoading,
            onClick = { onModeChange("MANUAL") }
        )
    }
}

/**
 * Строка с вариантом режима напоминания (RadioButton).
 */
@Composable
private fun ReminderModeRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
    }
}

/**
 * Слайдер для выбора количества дней до начала периода (для режима AUTO).
 */
@Composable
private fun ReminderDaysBeforeSlider(
    days: Int,
    isLoading: Boolean,
    onDaysChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "За сколько дней напоминать",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "$days ${getDaysWord(days)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = days.toFloat(),
            onValueChange = { onDaysChange(it.toInt()) },
            valueRange = 1f..7f,
            steps = 5, // 1, 2, 3, 4, 5, 6, 7
            enabled = !isLoading
        )

        Text(
            text = "Напоминание появится за $days ${getDaysWord(days)} до начала периода передачи показаний",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Список провайдеров с настройкой дня месяца для напоминания (для режима MANUAL).
 */
@Composable
private fun ProvidersRemindersList(
    providers: List<ProviderUiModel>,
    isLoading: Boolean,
    onProviderReminderDayChange: (Long, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "День напоминания для каждого поставщика",
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
                ProviderReminderDayRow(
                    providerUi = providerUi,
                    isLoading = isLoading,
                    onDayChange = onProviderReminderDayChange
                )
            }
        }
    }
}

/**
 * Строка с провайдером и выбором дня месяца для напоминания.
 */
@Composable
private fun ProviderReminderDayRow(
    providerUi: ProviderUiModel,
    isLoading: Boolean,
    onDayChange: (Long, Int) -> Unit
) {
    var showDayPicker by remember { mutableStateOf(false) }
    val currentDay = providerUi.provider.reminderCustomStartDay ?: 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = !isLoading) { showDayPicker = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Логотип
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(providerUi.provider.logoUrl)
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

        // Название
        Text(
            text = providerUi.provider.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // День месяца
        Text(
            text = "$currentDay число",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }

    if (showDayPicker) {
        DayPickerDialog(
            initialDay = currentDay,
            onConfirm = { selectedDay ->
                onDayChange(providerUi.provider.id, selectedDay)
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false }
        )
    }
}

/**
 * Склонение слова "день".
 */
private fun getDaysWord(days: Int): String {
    return when {
        days % 10 == 1 && days % 100 != 11 -> "день"
        days % 10 in 2..4 && days % 100 !in 12..14 -> "дня"
        else -> "дней"
    }
}

/**
 * Диалог выбора времени (Material3 TimePicker).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

/**
 * Список провайдеров с рассчитанными днями напоминаний (для режима AUTO).
 * Только для просмотра, редактирование недоступно.
 */
@Composable
private fun ProvidersRemindersAutoList(
    providers: List<ProviderUiModel>,
    daysBeforeStart: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Рассчитанные дни напоминаний",
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
                ProviderReminderAutoRow(
                    providerUi = providerUi,
                    daysBeforeStart = daysBeforeStart
                )
            }
        }
    }
}

/**
 * Строка с провайдером и рассчитанным днём напоминания (только просмотр).
 */
@Composable
private fun ProviderReminderAutoRow(
    providerUi: ProviderUiModel,
    daysBeforeStart: Int
) {
    val provider = providerUi.provider

    // Вычисляем день напоминания
    val reminderInfo = calculateReminderDay(
        periodStartDay = provider.transmissionPeriodStartDay,
        daysBeforeStart = daysBeforeStart
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Логотип
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(provider.logoUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_provider_placeholder)
                .error(R.drawable.ic_provider_placeholder)
                .build(),
            contentDescription = "Логотип ${provider.name}",
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Fit
        )

        // Название и информация о периоде
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodyMedium
            )

            // Информация о периоде и напоминании
            if (provider.transmissionPeriodStartDay != null && provider.transmissionPeriodEndDay != null) {
                Text(
                    text = "Период: ${provider.transmissionPeriodStartDay}-${provider.transmissionPeriodEndDay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Период не загружен",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Рассчитанный день напоминания
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = reminderInfo.displayText,
                style = MaterialTheme.typography.titleSmall,
                color = if (reminderInfo.isValid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            if (reminderInfo.isPreviousMonth) {
                Text(
                    text = "пред. мес.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Вспомогательный класс для информации о дне напоминания.
 */
private data class ReminderDayInfo(
    val day: Int,
    val isPreviousMonth: Boolean,
    val isValid: Boolean,
    val displayText: String
)

/**
 * Вычислить день напоминания на основе периода передачи.
 *
 * @param periodStartDay День начала периода (из БД провайдера)
 * @param daysBeforeStart За сколько дней до начала напоминать
 * @return Информация о дне напоминания
 */
private fun calculateReminderDay(
    periodStartDay: Int?,
    daysBeforeStart: Int
): ReminderDayInfo {
    if (periodStartDay == null) {
        return ReminderDayInfo(
            day = 0,
            isPreviousMonth = false,
            isValid = false,
            displayText = "—"
        )
    }

    val reminderDay = periodStartDay - daysBeforeStart

    return if (reminderDay >= 1) {
        // Напоминание в том же месяце
        ReminderDayInfo(
            day = reminderDay,
            isPreviousMonth = false,
            isValid = true,
            displayText = "$reminderDay число"
        )
    } else {
        // Напоминание в предыдущем месяце
        // Для упрощения считаем, что в месяце 30 дней
        val adjustedDay = 30 + reminderDay
        ReminderDayInfo(
            day = adjustedDay,
            isPreviousMonth = true,
            isValid = true,
            displayText = "$adjustedDay число"
        )
    }
}

