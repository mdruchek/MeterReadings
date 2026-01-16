// app/src/main/java/ru/dr/meterreadings/ui/screens/ProfileDetailScreen.kt

package ru.dr.meterreadings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import ru.dr.meterreadings.viewmodels.ProfileDetailViewModel
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.ui.components.MeterReadingInput

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
    // STATE ИЗ VIEWMODEL
    // ============================================

    LaunchedEffect(profileId) {
        viewModel.initialize(profileId)
    }

    val profile by viewModel.profile.collectAsState(initial = null)
    val accounts by viewModel.accounts.collectAsState()
    val meters by viewModel.meters.collectAsState()
    val accountAddresses by viewModel.accountAddresses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val submittingMeters by viewModel.submittingMeters.collectAsState()

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
                    // Кнопка обновления
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить"
                        )
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
        // СПИСОК СЧЁТЧИКОВ ПО АДРЕСАМ И АККАУНТАМ
        // ============================================

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Показываем загрузку
            if (isLoading && meters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Загрузка счётчиков...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Группируем счётчики по аккаунтам
            val metersByAccount = meters.groupBy { it.accountId }

            // Группируем аккаунты по адресам
            val accountsByAddress = accounts
                .mapNotNull { account ->
                    val address = accountAddresses[account.id]
                    if (address != null) account to address else null
                }
                .groupBy { it.second }  // Группируем по адресу

            // Отображаем по группам адресов
            accountsByAddress.forEach { (address, accountsWithAddress) ->

                // Заголовок адреса
                item(key = "address_$address") {
                    AddressHeader(
                        address = address,
                        accountCount = accountsWithAddress.size
                    )
                }

                // Аккаунты на этом адресе
                accountsWithAddress.forEach { (account, _) ->
                    val accountMeters = metersByAccount[account.id] ?: emptyList()

                    // Заголовок аккаунта (провайдер + номер ЛС)
                    item(key = "account_${account.id}") {
                        AccountHeader(
                            account = account,
                            meterCount = accountMeters.size,
                            onDelete = {
                                accountToDelete = account
                                showDeleteAccountDialog = true
                            }
                        )
                    }

                    // Счётчики аккаунта
                    if (accountMeters.isNotEmpty()) {
                        items(
                            items = accountMeters,
                            key = { it.id }
                        ) { meter ->
                            MeterReadingInput(
                                meter = meter,
                                onSubmit = { value ->
                                    viewModel.submitReading(meter, value)
                                },
                                isSubmitting = submittingMeters.contains(meter.id)
                            )
                        }
                    } else if (!isLoading) {
                        // Placeholder если нет счётчиков
                        item(key = "empty_${account.id}") {
                            EmptyMetersPlaceholder()
                        }
                    }

                    // Разделитель между аккаунтами
                    item(key = "spacer_${account.id}") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Отображаем аккаунты без адреса (если есть)
            val accountsWithoutAddress = accounts.filter { account ->
                accountAddresses[account.id] == null
            }

            if (accountsWithoutAddress.isNotEmpty()) {
                item(key = "no_address_header") {
                    AddressHeader(
                        address = "Адрес не загружен",
                        accountCount = accountsWithoutAddress.size
                    )
                }

                accountsWithoutAddress.forEach { account ->
                    val accountMeters = metersByAccount[account.id] ?: emptyList()

                    item(key = "account_noaddr_${account.id}") {
                        AccountHeader(
                            account = account,
                            meterCount = accountMeters.size,
                            onDelete = {
                                accountToDelete = account
                                showDeleteAccountDialog = true
                            }
                        )
                    }

                    if (accountMeters.isNotEmpty()) {
                        items(
                            items = accountMeters,
                            key = { it.id }
                        ) { meter ->
                            MeterReadingInput(
                                meter = meter,
                                onSubmit = { value ->
                                    viewModel.submitReading(meter, value)
                                },
                                isSubmitting = submittingMeters.contains(meter.id)
                            )
                        }
                    } else if (!isLoading) {
                        item(key = "empty_noaddr_${account.id}") {
                            EmptyMetersPlaceholder()
                        }
                    }

                    item(key = "spacer_noaddr_${account.id}") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
                        "Лицевой счёт № ${accountToDelete!!.accountNumber}\n" +
                                "Провайдер ID: ${accountToDelete!!.providerId}\n\n" +
                                "Все данные этого счёта будут удалены. Это действие нельзя отменить."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            accountToDelete?.let { account ->
                                viewModel.deleteAccount(account.id)
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
}

// ============================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ============================================

/**
 * Заголовок адреса с количеством счетов
 */
@Composable
fun AddressHeader(
    address: String,
    accountCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏠",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = address,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$accountCount ${if (accountCount == 1) "счёт" else "счетов"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Заголовок аккаунта (провайдер + номер ЛС)
 */
@Composable
fun AccountHeader(
    account: AccountDomainModel,
    meterCount: Int,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка (просто эмодзи по умолчанию)
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "№ ${account.accountNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (meterCount > 0) {
                    Text(
                        text = "$meterCount ${if (meterCount == 1) "счётчик" else if (meterCount < 5) "счётчика" else "счётчиков"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            // Меню с тремя точками
            Box {
                IconButton(onClick = { showMenu = true }) {
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
    }
}

/**
 * Placeholder для пустого списка счётчиков
 */
@Composable
fun EmptyMetersPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет счётчиков",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
