package ru.dr.meterreadings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.toUiModel
import ru.dr.meterreadings.models.ui.AuthError
import ru.dr.meterreadings.ui.components.MeterReadingInput
import ru.dr.meterreadings.viewmodels.ProfileDetailViewModel

/**
 * Экран детальной информации профиля со счётчиками
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profileId: String,
    navController: NavHostController,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountDomainModel?>(null) }

    // ============================================
    // STATE: Раскрывающиеся карточки
    // ============================================
    var expandedAccountId by remember { mutableStateOf<String?>(null) }
    var expandAllMode by remember { mutableStateOf(false) }

    // ============================================
    // STATE ИЗ VIEWMODEL
    // ============================================
    val profile by viewModel.profile.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountMeters by viewModel.accountMeters.collectAsState()
    val loadingAccounts by viewModel.loadingAccounts.collectAsState()
    val submittingMeters by viewModel.submittingMeters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val accountErrors by viewModel.accountErrors.collectAsState()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    // Snackbar для ошибок
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // ============================================
    // ДИАЛОГ ПЕРЕАУТЕНТИФИКАЦИИ (при истечении токена)
    // ============================================
    authError?.let { error ->
        ReauthDialog(
            authError = error,
            onDismiss = {
                viewModel.dismissAuthError()
            },
            onReauth = { login, password ->
                viewModel.reauthenticate(
                    login = login,
                    password = password,
                    onSuccess = {
                        viewModel.dismissAuthError()
                        // Автоматически перезагружаем данные для всех раскрытых карточек
                        accounts.forEach { account ->
                            val isExpanded = expandAllMode || expandedAccountId == account.id
                            if (isExpanded) {
                                viewModel.loadMetersForAccount(account.id)
                            }
                        }
                    },
                    onFailure = { errorMessage ->
                        viewModel.dismissAuthError()
                        viewModel.setError(errorMessage)
                    }
                )
            }
        )
    }

    // Показываем загрузку профиля
    if (profile == null && isLoading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Загрузка...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: "Детали профиля") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // ============================================
                    // КНОПКА "ОТКРЫТЬ ВСЕ"
                    // ============================================
                    if (accounts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                expandAllMode = !expandAllMode
                                if (!expandAllMode) {
                                    expandedAccountId = null
                                }
                            }
                        ) {
                            Text(if (expandAllMode) "Свернуть все" else "Открыть все")
                        }
                    }

                    // Меню
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать профиль") },
                            onClick = {
                                showMenu = false
                                // TODO: Открыть экран редактирования
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("add_account/$profileId")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить лицевой счёт"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // ============================================
        // ЕСЛИ НЕТ АККАУНТОВ - ПОКАЗЫВАЕМ PLACEHOLDER
        // ============================================
        if (accounts.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "📭",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Нет лицевых счетов",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Нажмите кнопку + чтобы добавить первый счёт",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        // ============================================
        // СПИСОК РАСКРЫВАЮЩИХСЯ КАРТОЧЕК АККАУНТОВ
        // ============================================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = accounts,
                key = { it.id }
            ) { account ->
                val isExpanded = expandAllMode || expandedAccountId == account.id
                val meters = accountMeters[account.id]
                val isLoadingMeters = loadingAccounts.contains(account.id)
                val error = accountErrors[account.id]

                // ============================================
                // АВТОМАТИЧЕСКАЯ ЗАГРУЗКА ПРИ РАСКРЫТИИ
                // ============================================
                LaunchedEffect(isExpanded) {
                    if (isExpanded && meters == null && !isLoadingMeters) {
                        println("📂 [ProfileDetailScreen] Карточка ${account.id} раскрылась → загружаем счётчики")
                        viewModel.loadMetersForAccount(account.id)
                    }
                }

                ExpandableAccountCard(
                    account = account,
                    meterCount = meters?.size ?: 0,
                    isExpanded = isExpanded,
                    isLoading = isLoadingMeters,
                    expandAllMode = expandAllMode,
                    onCardClick = {
                        if (!expandAllMode) {
                            // Accordion: только одна карточка открыта
                            expandedAccountId = if (expandedAccountId == account.id) {
                                null
                            } else {
                                account.id
                            }
                        }
                    },
                    onDelete = {
                        accountToDelete = account
                        showDeleteAccountDialog = true
                    },
                    content = {
                        when {
                            // Загружается
                            isLoadingMeters -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            // ✅ Показываем ошибку если есть
                            error != null -> {
                                ErrorMetersPlaceholder(
                                    errorMessage = error,
                                    onRetry = {
                                        viewModel.clearAccountError(account.id)
                                        viewModel.loadMetersForAccount(account.id)
                                    }
                                )
                            }

                            // Загружены счётчики
                            meters != null && meters.isNotEmpty() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    meters.forEach { meter ->
                                        // ✅ ПРОВЕРЯЕМ: отправляется ли этот счётчик
                                        val isSubmitting = submittingMeters[account.id]?.contains(meter.id) == true

                                        MeterReadingInput(
                                            meter = meter.toUiModel(),
                                            onSubmit = { value ->
                                                viewModel.submitReading(meter.toUiModel(), value)
                                            },
                                            isSubmitting = isSubmitting
                                        )
                                    }
                                }
                            }

                            // Пусто
                            meters != null && meters.isEmpty() -> {
                                EmptyMetersPlaceholder()
                            }
                        }
                    }
                )
            }
        }
    }

    // ============================================
    // ДИАЛОГ УДАЛЕНИЯ АККАУНТА
    // ============================================
    if (showDeleteAccountDialog && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAccountDialog = false
                accountToDelete = null
            },
            title = { Text("Удалить лицевой счёт?") },
            text = {
                Text(
                    "Лицевой счёт № ${accountToDelete!!.number}\n" +
                            "Провайдер ID: ${accountToDelete!!.providerId}\n\n" +
                            "Все данные этого счёта будут удалены. Это действие нельзя отменить."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToDelete?.let { account ->
                            viewModel.deleteAccount(account.id)
                            println("🗑️ Удаление аккаунта ${account.id}")
                        }
                        showDeleteAccountDialog = false
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        accountToDelete = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

// ============================================
// ДИАЛОГ ПЕРЕАУТЕНТИФИКАЦИИ
// ============================================
@Composable
fun ReauthDialog(
    authError: AuthError,
    onDismiss: () -> Unit,
    onReauth: (login: String, password: String) -> Unit
) {
    var login by remember {
        mutableStateOf(
            when (authError) {
                is AuthError.TokenExpiredNoRefresh -> authError.login
                is AuthError.RefreshFailed -> authError.login
                else -> ""
            }
        )
    }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(authError.title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(authError.message)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ✅ Дополнительная информация для разработчика
                if (authError is AuthError.TokenExpiredNoRefresh) {
                    Text(
                        text = "ℹ️ Для разработчика:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Реализуйте TnsRepository.refreshAccessToken() для автоматического обновления.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Форма входа
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Email") },
                    placeholder = { Text("example@mail.ru") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (login.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        onReauth(login, password)
                    }
                },
                enabled = login.isNotBlank() && password.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isLoading) "Вход..." else "Войти")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Отмена")
            }
        }
    )
}

// ============================================
// РАСКРЫВАЮЩАЯСЯ КАРТОЧКА АККАУНТА
// ============================================
@Composable
fun ExpandableAccountCard(
    account: AccountDomainModel,
    meterCount: Int,
    isExpanded: Boolean,
    isLoading: Boolean,
    expandAllMode: Boolean,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Анимация поворота стрелки
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            ),
        onClick = onCardClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ============================================
            // ЗАГОЛОВОК КАРТОЧКИ (всегда видно)
            // ============================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка
                Text(
                    text = "💧",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )

                // Информация об аккаунте
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Провайдер ${account.providerId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "№ ${account.number}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    if (meterCount > 0) {
                        Text(
                            text = "$meterCount ${
                                when {
                                    meterCount == 1 -> "счётчик"
                                    meterCount < 5 -> "счётчика"
                                    else -> "счётчиков"
                                }
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }

                // Стрелка раскрытия (только если не режим "Открыть все")
                if (!expandAllMode) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                        modifier = Modifier.rotate(rotationState),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Меню с тремя точками
                Box {
                    IconButton(
                        onClick = {
                            showMenu = true
                        }
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Удалить",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            // ============================================
            // СОДЕРЖИМОЕ КАРТОЧКИ (счётчики)
            // ============================================
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    content()
                }
            }
        }
    }
}

// ============================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ============================================

/**
 * Placeholder для пустого списка счётчиков
 */
@Composable
fun EmptyMetersPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Нет счётчиков",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

/**
 * Placeholder для ошибки загрузки счётчиков
 */
@Composable
fun ErrorMetersPlaceholder(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "❌",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "Ошибка загрузки",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Повторить")
        }
    }
}
