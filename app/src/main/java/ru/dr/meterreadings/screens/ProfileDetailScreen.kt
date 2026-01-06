package ru.dr.meterreadings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import ru.dr.meterreadings.viewmodels.ProfileDetailViewModel  // ← ИЗМЕНИЛИ!
import ru.dr.meterreadings.models.ui.AccountUiModel
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.AuthType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profileId: String,
    navController: NavHostController,
    viewModel: ProfileDetailViewModel = hiltViewModel()  // ← ИЗМЕНИЛИ!
) {
    var showMenu by remember { mutableStateOf(false) }

    // =====================================================
    // STATE ИЗ VIEWMODEL (вместо моков!)
    // =====================================================
    val profile by viewModel.profile.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Обработка ошибок
    LaunchedEffect(error) {
        error?.let {
            println("❌ Ошибка: $it")
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
    // СПРАВОЧНИК ПРОВАЙДЕРОВ (пока моковый)
    // =====================================================
    val providers = remember {
        mapOf(
            "provider_1" to ProviderDomainModel(
                id = "provider_1",
                name = "Управляющая компания №1",
                type = "ЖКХ",
                logoUrl = null,
                baseUrl = "https://uk1.ru",
                authType = AuthType.FORM_CSRF
            ),
            "provider_2" to ProviderDomainModel(
                id = "provider_2",
                name = "Энергосбыт",
                type = "Электричество",
                logoUrl = null,
                baseUrl = "https://energosbyt.ru",
                authType = AuthType.API_KEY
            ),
            "provider_3" to ProviderDomainModel(
                id = "provider_3",
                name = "Газпром Межрегионгаз",
                type = "Газ",
                logoUrl = null,
                baseUrl = "https://gazprom.ru",
                authType = AuthType.AUTH_REQUIRED
            )
        )
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
                        DropdownMenuItem(
                            text = { Text("Добавить снабжающую кампанию") },
                            onClick = {
                                showMenu = false
                                navController.navigate("add_account/$profileId")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )

                        Divider()

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
                        text = "Добавьте первый счёт через меню",
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
                                }
                            )
                        }
                    }
                }
            }
        }
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
    onClick: () -> Unit
) {
    val icon = when (provider.type) {
        "ЖКХ" -> "🏢"
        "Электричество" -> "⚡"
        "Газ" -> "🔥"
        "Водоснабжение" -> "💧"
        else -> "📋"
    }

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
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.type,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "№ ${accountUi.account.accountNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${accountUi.meters.size} счетчиков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
