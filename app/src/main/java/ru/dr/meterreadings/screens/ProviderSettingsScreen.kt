// app/src/main/java/ru/dr/meterreadings/screens/ProviderSettingsScreen.kt

package ru.dr.meterreadings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.dr.meterreadings.viewmodels.ProviderSettingsViewModel

/**
 * Экран настроек провайдера в стиле Material 3 Settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    providerId: String,
    navController: NavController,
    viewModel: ProviderSettingsViewModel = hiltViewModel()
) {
    val provider by viewModel.provider.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Инициализация
    LaunchedEffect(providerId) {
        viewModel.loadProvider(providerId)
    }

    // SnackBar для сообщений
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar(
                message = "Настройки сохранены",
                duration = SnackbarDuration.Short
            )
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки ${provider?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (provider == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ГРУППА: Напоминания
                SettingsGroup(title = "Напоминания") {
                    ReminderSettings(
                        provider = provider!!,
                        viewModel = viewModel,
                        isLoading = isLoading
                    )
                }

                HorizontalDivider()

                // ГРУППА: Автообновление
                SettingsGroup(title = "Автообновление данных") {
                    AutoUpdateSettings(
                        provider = provider!!,
                        viewModel = viewModel,
                        isLoading = isLoading
                    )
                }

                HorizontalDivider()

                // ГРУППА: Уведомления
                SettingsGroup(title = "Уведомления") {
                    NotificationSettings(
                        provider = provider!!,
                        viewModel = viewModel,
                        isLoading = isLoading
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================
// ГРУППЫ НАСТРОЕК
// ============================================

/**
 * Группа настроек с заголовком
 */
//@Composable
//fun SettingsGroup(
//    title: String,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    Column(modifier = Modifier.fillMaxWidth()) {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.titleSmall,
//            color = MaterialTheme.colorScheme.primary,
//            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//        )
//        content()
//    }
//}

/**
 * Настройки напоминаний
 */
@Composable
fun ReminderSettings(
    provider: ru.dr.meterreadings.models.domain.ProviderDomainModel,
    viewModel: ProviderSettingsViewModel,
    isLoading: Boolean
) {
    var reminderEnabled by remember { mutableStateOf(provider.reminderEnabled) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(provider.reminderTimeHour) }
    var selectedMinute by remember { mutableStateOf(provider.reminderTimeMinute) }
    var showPeriodModeDialog by remember { mutableStateOf(false) }
    var showCustomDayPicker by remember { mutableStateOf(false) }
    var periodMode by remember { mutableStateOf(provider.reminderPeriodMode) }
    var customStartDay by remember { mutableStateOf(provider.reminderCustomStartDay ?: 17) }
    var customEndDay by remember { mutableStateOf(provider.reminderCustomEndDay ?: 25) }

    // Переключатель напоминаний
    SettingsSwitchItem(
        icon = Icons.Default.Notifications,
        title = "Напоминать о передаче показаний",
        subtitle = if (reminderEnabled) {
            "Ежедневно в ${String.format("%02d:%02d", selectedHour, selectedMinute)}"
        } else {
            "Отключено"
        },
        checked = reminderEnabled,
        enabled = !isLoading,
        onCheckedChange = { enabled ->
            reminderEnabled = enabled
            viewModel.updateReminderSettings(
                enabled = enabled,
                hour = selectedHour,
                minute = selectedMinute
            )
        }
    )

    if (reminderEnabled) {
        // Выбор времени
        SettingsClickableItem(
            icon = Icons.Default.Schedule,
            title = "Время напоминания",
            subtitle = String.format("%02d:%02d", selectedHour, selectedMinute),
            enabled = !isLoading,
            onClick = { showTimePicker = true }
        )

        // Период напоминаний
        val periodText = when (periodMode) {
            "AUTO" -> {
                val start = provider.transmissionPeriodStartDay ?: 17
                val end = provider.transmissionPeriodEndDay ?: 25
                "Автоматически ($start-$end число)"
            }
            "MANUAL" -> "Настроить вручную ($customStartDay-$customEndDay)"
            else -> "Не выбрано"
        }

        SettingsClickableItem(
            icon = Icons.Default.DateRange,
            title = "Период напоминаний",
            subtitle = periodText,
            enabled = !isLoading,
            onClick = { showPeriodModeDialog = true }
        )
    }

    // TimePicker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                viewModel.updateReminderSettings(
                    enabled = reminderEnabled,
                    hour = hour,
                    minute = minute
                )
                showTimePicker = false
            }
        )
    }

    // Диалог выбора режима периода
    if (showPeriodModeDialog) {
        PeriodModeDialog(
            currentMode = periodMode,
            onDismiss = { showPeriodModeDialog = false },
            onModeSelected = { mode ->
                periodMode = mode
                if (mode == "AUTO") {
                    viewModel.updateReminderPeriodMode(
                        mode = "AUTO",
                        customStartDay = null,
                        customEndDay = null
                    )
                } else {
                    showCustomDayPicker = true
                }
                showPeriodModeDialog = false
            }
        )
    }

    // Диалог выбора дней (MANUAL)
    if (showCustomDayPicker) {
        CustomDayPickerDialog(
            startDay = customStartDay,
            endDay = customEndDay,
            onDismiss = { showCustomDayPicker = false },
            onConfirm = { start, end ->
                customStartDay = start
                customEndDay = end
                viewModel.updateReminderPeriodMode(
                    mode = "MANUAL",
                    customStartDay = start,
                    customEndDay = end
                )
                showCustomDayPicker = false
            }
        )
    }
}

/**
 * Настройки автообновления
 */
@Composable
fun AutoUpdateSettings(
    provider: ru.dr.meterreadings.models.domain.ProviderDomainModel,
    viewModel: ProviderSettingsViewModel,
    isLoading: Boolean
) {
    var autoUpdateEnabled by remember { mutableStateOf(provider.autoUpdateEnabled) }
    var updateIntervalHours by remember { mutableStateOf(provider.updateIntervalHours) }

    // Переключатель автообновления
    SettingsSwitchItem(
        icon = Icons.Default.Refresh,
        title = "Автоматическое обновление счётчиков",
        subtitle = if (autoUpdateEnabled) {
            "Каждые $updateIntervalHours ч"
        } else {
            "Отключено"
        },
        checked = autoUpdateEnabled,
        enabled = !isLoading,
        onCheckedChange = { enabled ->
            autoUpdateEnabled = enabled
            viewModel.updateAutoUpdateSettings(
                enabled = enabled,
                intervalHours = updateIntervalHours
            )
        }
    )

    // Интервал обновления
    if (autoUpdateEnabled) {
        SettingsSliderItem(
            icon = Icons.Default.Timer,
            title = "Интервал обновления",
            subtitle = "$updateIntervalHours ч",
            value = updateIntervalHours.toFloat(),
            valueRange = 1f..24f,
            steps = 22,
            enabled = !isLoading,
            onValueChangeFinished = { newValue ->
                updateIntervalHours = newValue.toInt()
                viewModel.updateAutoUpdateSettings(
                    enabled = autoUpdateEnabled,
                    intervalHours = newValue.toInt()
                )
            }
        )
    }
}

/**
 * Настройки уведомлений
 */
@Composable
fun NotificationSettings(
    provider: ru.dr.meterreadings.models.domain.ProviderDomainModel,
    viewModel: ProviderSettingsViewModel,
    isLoading: Boolean
) {
    var updateNotificationsEnabled by remember { mutableStateOf(provider.updateNotificationsEnabled) }
    var errorNotificationsEnabled by remember { mutableStateOf(provider.errorNotificationsEnabled) }

    SettingsSwitchItem(
        icon = Icons.Default.CheckCircle,
        title = "Уведомления о передаче показаний",
        subtitle = if (updateNotificationsEnabled) {
            "Показывать при успешной передаче"
        } else {
            "Отключено"
        },
        checked = updateNotificationsEnabled,
        enabled = !isLoading,
        onCheckedChange = { enabled ->
            updateNotificationsEnabled = enabled
            viewModel.updateNotificationSettings(
                updateEnabled = enabled,
                errorEnabled = errorNotificationsEnabled
            )
        }
    )

    SettingsSwitchItem(
        icon = Icons.Default.Error,
        title = "Уведомления об ошибках",
        subtitle = if (errorNotificationsEnabled) {
            "Показывать при неудачной передаче"
        } else {
            "Отключено"
        },
        checked = errorNotificationsEnabled,
        enabled = !isLoading,
        onCheckedChange = { enabled ->
            errorNotificationsEnabled = enabled
            viewModel.updateNotificationSettings(
                updateEnabled = updateNotificationsEnabled,
                errorEnabled = enabled
            )
        }
    )
}

// ============================================
// UI КОМПОНЕНТЫ
// ============================================

/**
 * Элемент настроек с переключателем
 */
@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Кликабельный элемент настроек
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

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

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Элемент настроек со слайдером
 */
@Composable
fun SettingsSliderItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (Float) -> Unit
) {
    var currentValue by remember { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
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
        }

        Slider(
            value = currentValue,
            onValueChange = { currentValue = it },
            onValueChangeFinished = { onValueChangeFinished(currentValue) },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

// ============================================
// ДИАЛОГИ
// ============================================

/**
 * Диалог выбора времени
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
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
                Text("ОК")
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
 * Диалог выбора режима периода напоминаний
 */
@Composable
fun PeriodModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Период напоминаний") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "AUTO",
                        onClick = { selectedMode = "AUTO" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Автоматически", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Период загружается с сервера",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == "MANUAL",
                        onClick = { selectedMode = "MANUAL" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Настроить вручную", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Выбрать свой период",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onModeSelected(selectedMode) }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Диалог выбора дней (MANUAL режим)
 */
@Composable
fun CustomDayPickerDialog(
    startDay: Int,
    endDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (startDay: Int, endDay: Int) -> Unit
) {
    var selectedStartDay by remember { mutableStateOf(startDay.toFloat()) }
    var selectedEndDay by remember { mutableStateOf(endDay.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите период") },
        text = {
            Column {
                Text("День начала: ${selectedStartDay.toInt()}")
                Slider(
                    value = selectedStartDay,
                    onValueChange = { selectedStartDay = it },
                    valueRange = 1f..31f,
                    steps = 29
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("День окончания: ${selectedEndDay.toInt()}")
                Slider(
                    value = selectedEndDay,
                    onValueChange = { selectedEndDay = it },
                    valueRange = 1f..31f,
                    steps = 29
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedStartDay.toInt(), selectedEndDay.toInt())
            }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
