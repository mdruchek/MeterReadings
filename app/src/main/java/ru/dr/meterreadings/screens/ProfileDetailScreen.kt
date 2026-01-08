package ru.dr.meterreadings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import ru.dr.meterreadings.viewmodels.ProfileDetailViewModel  // ← ИЗМЕНИЛИ!
import ru.dr.meterreadings.models.ui.AccountUiModel
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.Type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profileId: String,
    navController: NavHostController,
    viewModel: ProfileDetailViewModel = hiltViewModel()  // ← ИЗМЕНИЛИ!
) {
    var showMenu by remember { mutableStateOf(false) }

    // =====================================================
    // STATE ДЛЯ УДАЛЕНИЯ АККАУНТА
    // =====================================================

    // Показывать ли диалог подтверждения удаления
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Какой аккаунт хотим удалить (сохраняем для диалога)
    var accountToDelete by remember { mutableStateOf<AccountUiModel?>(null) }

    // =====================================================
    // STATE ИЗ VIEWMODEL (вместо моков!)
    // =====================================================
    val profile by viewModel.profile.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val providers by viewModel.providers.collectAsStateWithLifecycle()

    // Обработка ошибок
    LaunchedEffect(error) {
        error?.let {
            println("❌ [ProfileDetailScreen] Ошибка: $it")
            viewModel.clearError()
        }
    }

    // Показываем загрузку или "профиль не найден"
    if (profile == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Детали профиля") },
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
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Профиль не найден")
                }
            }
        }
        return
    }

    // =====================================================
    // ПРЕОБРАЗУЕМ ACCOUNTS В AccountUiModel
    // =====================================================
    val accountsUi = remember(accounts) {
        accounts.map { account ->
            AccountUiModel(
                account = account,
                address = "Адрес не указан",  // TODO: добавить Address в БД
                lastUpdated = null, // ← null пока не парсим сайты
                meters = emptyList()  // TODO: загрузить счетчики
            )
        }
    }

    // Группируем по адресам
    val accountsByAddress = accountsUi
        .filter { it.address != null }
        .groupBy { it.address!! }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: "Загрузка...") },  // ← Реальное имя!
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
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
                        // ❌ УБРАЛИ "Добавить счёт" (теперь через FAB)

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
        // ✅ ДОБАВИЛИ FAB ДЛЯ ДОБАВЛЕНИЯ СЧЁТА
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
        }
    ) { paddingValues ->
        // =====================================================
        // ЕСЛИ НЕТ АККАУНТОВ - ПОКАЗЫВАЕМ PLACEHOLDER
        // =====================================================
        if (accountsUi.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📭",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Нет счетов",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Нажмите кнопку + чтобы добавить первый счёт",  // ← Обновили подсказку
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // =====================================================
            // СПИСОК АККАУНТОВ
            // =====================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                accountsByAddress.forEach { (address, accountsAtAddress) ->
                    item {
                        AddressHeader(
                            address = address,
                            accountCount = accountsAtAddress.size
                        )
                    }

                    items(accountsAtAddress) { accountUi ->
                        val provider = providers[accountUi.account.providerId]
                        if (provider != null) {
                            AccountCard(
                                accountUi = accountUi,
                                provider = provider,
                                onClick = {
                                    // TODO: Открыть детали счета
                                },
                                // ✅ ПАРАМЕТР - что делать при удалении
                                onDelete = {
                                    // Сохраняем данные счёта и показываем диалог подтверждения
                                    accountToDelete = accountUi
                                    showDeleteAccountDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // =====================================================
    // ДИАЛОГ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ АККАУНТА
    // =====================================================
    if (showDeleteAccountDialog && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                // Закрыть диалог при клике вне его
                showDeleteAccountDialog = false
                accountToDelete = null
            },
            title = {
                Text("Удалить лицевой счёт?")
            },
            text = {
                // Показываем информацию о удаляемом счёте
                val provider = providers[accountToDelete!!.account.providerId]
                Text(
                    "Лицевой счёт № ${accountToDelete!!.account.accountNumber}\n" +
                            "${provider?.name ?: "Неизвестный провайдер"}\n\n" +
                            "Все данные этого счёта будут удалены. Это действие нельзя отменить."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Вызываем метод удаления из ViewModel
                        accountToDelete?.let { account ->
                            viewModel.deleteAccount(account.account.id)
                        }

                        // Закрываем диалог
                        showDeleteAccountDialog = false
                        accountToDelete = null
                    },
                    // Красная кнопка для акцента опасного действия
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
                        // Отмена - просто закрываем диалог
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

@Composable
fun AddressHeader(
    address: String,
    accountCount: Int
) {
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
            Text(
                text = "🏠",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column {
                Text(
                    text = address,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "$accountCount счетов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    accountUi: AccountUiModel,
    provider: ProviderDomainModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // ✅ ИЗМЕНЕНИЕ: Используем enum Type вместо строк
    val icon = when (provider.type) {
        Type.WaterSupply -> "💧"
        Type.ElectricitySupply -> "⚡"
        Type.GasSupply -> "🔥"
        // Если добавите новые типы в enum, компилятор заставит обработать их здесь!
    }

    // ✅ STATE - показывать/скрывать меню с тремя точками
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка провайдера (эмодзи)
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )

            // Информация о счёте
            Column(modifier = Modifier.weight(1f)) {
                // Тип провайдера (ЖКХ, Газ, etc)
                Text(
                    text = provider.type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Название провайдера
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Номер лицевого счёта
                Text(
                    text = "№ ${accountUi.account.accountNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Количество счётчиков
                Text(
                    text = "${accountUi.meters.size} счетчиков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // =====================================================
            // ✅ НОВОЕ - МЕНЮ С ТРЕМЯ ТОЧКАМИ (как в ProfileCard)
            // =====================================================
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Выпадающее меню
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Пункт "Удалить" (красным цветом для акцента)
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Удалить",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete() // ← Вызываем callback
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
