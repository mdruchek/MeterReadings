@file:OptIn(ExperimentalMaterial3Api::class)
package ru.dr.meterreadings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dr.meterreadings.domain.connector.GetAccounts
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.domain.Type
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.ui.components.ErrorDialog
import ru.dr.meterreadings.ui.components.CaptchaDialog
import ru.dr.meterreadings.viewmodels.AddAccountViewModel

@Composable
fun AddAccountWizard(
    profile: ProfileDomainModel,
    onCancel: () -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel()
) {
    // =====================================================
    // STATE
    // =====================================================

    var currentStep by remember { mutableStateOf(1) }
    var accountNumber by remember { mutableStateOf("") }

    var regionSearchQuery by remember { mutableStateOf("") }
    var isRegionDropdownExpanded by remember { mutableStateOf(false) }

    // State из ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredProviders by viewModel.filteredProviders.collectAsStateWithLifecycle()
    val selectedProviderId by viewModel.selectedProviderId.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.getSelectedProvider().collectAsStateWithLifecycle()

    val providerHasRegions by viewModel.providerHasRegions.collectAsStateWithLifecycle()
    val regions by viewModel.regions.collectAsStateWithLifecycle()
    val selectedRegionId by viewModel.selectedRegionId.collectAsStateWithLifecycle()

    val isLoadingRegions by viewModel.isLoadingRegions.collectAsStateWithLifecycle()

    val searchedAccounts by viewModel.searchedAccounts.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val shouldResetToStep1 by viewModel.shouldResetToStep1.collectAsStateWithLifecycle()

    val isCreating by viewModel.isCreating.collectAsStateWithLifecycle()
    val createdAccountId by viewModel.createdAccountId.collectAsStateWithLifecycle()

    val authData by viewModel.authData.collectAsStateWithLifecycle()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Капча
    val showCaptcha by viewModel.showCaptcha.collectAsStateWithLifecycle()
    val captchaUrl by viewModel.captchaUrl.collectAsStateWithLifecycle()

    // ✅ МНОЖЕСТВЕННЫЙ ВЫБОР
    var selectedAccountNumbers by remember { mutableStateOf<Set<String>>(emptySet()) }

    // ✅ Автоматически выбираем все аккаунты при загрузке
    LaunchedEffect(searchedAccounts) {
        searchedAccounts?.let { accounts ->
            selectedAccountNumbers = accounts.map { it.accountNumber }.toSet()
        }
    }

    LaunchedEffect(searchedAccounts) {
        if (searchedAccounts != null && currentStep == 2) {
            currentStep = 3
        }
    }

    // =====================================================
    // АВТОМАТИЧЕСКАЯ НАВИГАЦИЯ ПОСЛЕ СОЗДАНИЯ
    // =====================================================

    LaunchedEffect(createdAccountId) {
        if (createdAccountId != null) {
            println("✅ [Wizard] Аккаунт создан: $createdAccountId, закрываем wizard")
            onCancel()
            viewModel.resetCreation()
        }
    }

    // =====================================================
    // АВТОМАТИЧЕСКАЯ ОЧИСТКА СОСТОЯНИЯ
    // =====================================================

    LaunchedEffect(shouldResetToStep1) {
        if (shouldResetToStep1) {
            currentStep = 1
            accountNumber = ""
            viewModel.resetCompleted()
        }
    }

    LaunchedEffect(currentStep) {
        println("🔄 [AddAccountWizard] Переход на шаг $currentStep")
        when (currentStep) {
            1 -> {
                println("🧹 [AddAccountWizard] Полная очистка состояния")
                accountNumber = ""
                login = ""
                password = ""
                regionSearchQuery = ""
                isRegionDropdownExpanded = false
            }
            3 -> {
                isRegionDropdownExpanded = false
            }
        }
    }

    // =====================================================
    // ДИАЛОГ ОШИБКИ
    // =====================================================

    errorState?.let { error ->
        ErrorDialog(
            title = error.title,
            message = error.message,
            onDismiss = {
                viewModel.dismissError()
                viewModel.clearSelection()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(currentStep) {
                            1 -> "Выберите компанию"
                            2 -> "Лицевой счет"
                            3 -> "Подтверждение"
                            else -> "Добавление"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "Отмена")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentStep = currentStep,
                totalSteps = 3,
                canGoBack = currentStep > 1,
                canGoNext = when(currentStep) {
                    1 -> selectedProviderId != null
                    2 -> {
                        when (selectedProvider?.authType) {
                            AuthType.ACCOUNT_NUMBER -> {
                                val hasAccountNumber = accountNumber.isNotBlank()
                                val hasRegionIfNeeded = !providerHasRegions || selectedRegionId != null
                                hasAccountNumber && hasRegionIfNeeded
                            }
                            AuthType.LOGIN_PASSWORD -> {
                                val hasCredentials = login.isNotBlank() && password.isNotBlank()
                                val hasRegionIfNeeded = !providerHasRegions || selectedRegionId != null
                                hasCredentials && hasRegionIfNeeded
                            }
                            null -> false
                        }
                    }
                    3 -> selectedAccountNumbers.isNotEmpty() && !isCreating
                    else -> false
                },
                onNext = {
                    when(currentStep) {
                        1 -> {
                            if (selectedProviderId != null) {
                                viewModel.getRegionsForProvider(selectedProviderId!!)
                                currentStep = 2
                            }
                        }
                        2 -> {
                            if (selectedProviderId != null) {
                                when (selectedProvider?.authType) {
                                    AuthType.ACCOUNT_NUMBER -> {
                                        if (accountNumber.isNotBlank()) {
                                            viewModel.getAccounts(
                                                providerId = selectedProviderId!!,
                                                accountNumber = accountNumber,
                                                regionId = selectedRegionId,
                                                login = null
                                            )
                                            // ✅ НЕ переходим на шаг 3 сразу!
                                            // Переход будет после успешного getAccounts()
                                        }
                                    }
                                    AuthType.LOGIN_PASSWORD -> {
                                        if (login.isNotBlank() && password.isNotBlank()) {
                                            viewModel.authorizeUser(
                                                providerId = selectedProviderId!!,
                                                login = login,
                                                password = password,
                                                regionId = selectedRegionId
                                            )
                                            // ✅ Тоже не переходим сразу
                                        }
                                    }
                                    null -> {
                                        // Ошибка
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (selectedProviderId != null) {
                                // ✅ Фильтруем только выбранные аккаунты
                                val accountsToAdd = searchedAccounts?.filter {
                                    selectedAccountNumbers.contains(it.accountNumber)
                                }

                                if (accountsToAdd.isNullOrEmpty()) {
                                    viewModel.showError(
                                        title = "Выберите аккаунты",
                                        message = "Пожалуйста, выберите хотя бы один аккаунт"
                                    )
                                } else {
                                    // ✅ Добавляем все выбранные аккаунты
                                    viewModel.createAccounts(
                                        profileId = profile.id,
                                        accountsInfo = accountsToAdd
                                    )
                                }
                            }
                        }
                    }
                },
                onBack = {
                    if (currentStep > 1) {
                        if (currentStep == 3) {
                            viewModel.clearSearchResult()
                        }
                        currentStep--
                    }
                },
                isLoading = isCreating,
                authType = selectedProvider?.authType
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when(currentStep) {
                // ============================================
                // ШАГ 1: Выбор провайдера
                // ============================================
                1 -> {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Поиск компании") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    items(filteredProviders) { provider ->
                        ProviderCard(
                            provider = provider,
                            isSelected = provider.provider.id == selectedProviderId,
                            onClick = {
                                viewModel.selectProvider(provider.provider.id)
                            }
                        )
                    }
                }

                // ============================================
                // ШАГ 2: Ввод ЛС + выбор региона (если нужно)
                // ============================================
                2 -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val providerName = selectedProvider?.name ?: "Провайдер"
                            val providerEmoji = when(selectedProvider?.type) {
                                Type.WaterSupply -> "💧"
                                Type.GasSupply -> "🔥"
                                Type.ElectricitySupply -> "⚡"
                                null -> "📋"
                            }

                            Text(
                                text = "$providerEmoji $providerName",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Введите данные",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isLoadingRegions) {
                        item {
                            Spacer(Modifier.height(32.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Загрузка регионов...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    else {
                        if (providerHasRegions && regions.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Регион",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))

                                ExposedDropdownMenuBox(
                                    expanded = isRegionDropdownExpanded,
                                    onExpandedChange = { isRegionDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = regions.find { it.id == selectedRegionId }?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Выберите регион") },
                                        placeholder = { Text("Нажмите для выбора") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = isRegionDropdownExpanded
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isRegionDropdownExpanded,
                                        onDismissRequest = {
                                            isRegionDropdownExpanded = false
                                            regionSearchQuery = ""
                                        },
                                        modifier = Modifier.heightIn(max = 400.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = regionSearchQuery,
                                            onValueChange = { regionSearchQuery = it },
                                            placeholder = { Text("Поиск региона...") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            singleLine = true
                                        )

                                        HorizontalDivider()

                                        val filteredRegions = if (regionSearchQuery.isBlank()) {
                                            regions
                                        } else {
                                            regions.filter { region ->
                                                region.name.contains(regionSearchQuery, ignoreCase = true)
                                            }
                                        }

                                        if (filteredRegions.isEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Регион не найден",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                },
                                                onClick = {},
                                                enabled = false
                                            )
                                        } else {
                                            filteredRegions.forEach { region ->
                                                DropdownMenuItem(
                                                    text = { Text(region.name) },
                                                    onClick = {
                                                        viewModel.selectRegion(region.id)
                                                        isRegionDropdownExpanded = false
                                                        regionSearchQuery = ""
                                                    },
                                                    leadingIcon = if (region.id == selectedRegionId) {
                                                        {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(24.dp))

                            when (selectedProvider?.authType) {
                                AuthType.ACCOUNT_NUMBER -> {
                                    // Номер лицевого счёта
                                    Text(
                                        text = "Лицевой счёт",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = accountNumber,
                                        onValueChange = { accountNumber = it },
                                        label = { Text("Номер счёта") },
                                        placeholder = { Text("123456789") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                AuthType.LOGIN_PASSWORD -> {
                                    // Логин и пароль
                                    Text(
                                        text = "Данные для входа",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = login,
                                        onValueChange = { login = it },
                                        label = { Text("Email или логин") },
                                        placeholder = { Text("example@mail.ru") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Пароль") },
                                        placeholder = { Text("••••••••") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                null -> {}
                            }
                        }
                    }
                }

                // ============================================
                // ШАГ 3: Подтверждение
                // ============================================
                3 -> {
                    item {
                        when {
                            isSearching -> {
                                // Загрузка...
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = when (selectedProvider?.authType) {
                                                AuthType.LOGIN_PASSWORD -> "Авторизация..."
                                                else -> "Поиск аккаунта..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            searchedAccounts != null -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Выберите аккаунты для добавления",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    // ✅ КНОПКА "Выбрать всё"
                                    TextButton(
                                        onClick = {
                                            selectedAccountNumbers = if (selectedAccountNumbers.size == searchedAccounts!!.size) {
                                                emptySet() // Снять все
                                            } else {
                                                searchedAccounts!!.map { it.accountNumber }.toSet() // Выбрать все
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (selectedAccountNumbers.size == searchedAccounts!!.size) {
                                                "Снять все"
                                            } else {
                                                "Выбрать все"
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Выбрано: ${selectedAccountNumbers.size} из ${searchedAccounts!!.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ✅ СПИСОК АККАУНТОВ С ЧЕКБОКСАМИ
                    searchedAccounts?.let { accounts ->
                        items(accounts) { account ->
                            AccountCard(
                                account = account,
                                isSelected = selectedAccountNumbers.contains(account.accountNumber),
                                onToggle = {
                                    selectedAccountNumbers = if (selectedAccountNumbers.contains(account.accountNumber)) {
                                        selectedAccountNumbers - account.accountNumber // Убрать
                                    } else {
                                        selectedAccountNumbers + account.accountNumber // Добавить
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // ==================== МОДАЛЬНОЕ ОКНО КАПЧИ ====================
    if (showCaptcha) {
        CaptchaDialog(
            onCaptchaCompleted = { session ->
                println("✅ [AddAccountWizard] Got fresh captcha token")
                viewModel.onCaptchaCompleted(session)
            },
            onDismiss = {
                println("⚠️ [AddAccountWizard] Captcha dialog dismissed")
                viewModel.dismissCaptcha()
            }
        )
    }
}

@Composable
fun ProviderCard(
    provider: ProviderUiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when(provider.provider.type) {
                    Type.WaterSupply -> "💧"
                    Type.GasSupply -> "🔥"
                    Type.ElectricitySupply -> "⚡"
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.provider.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = when(provider.provider.type) {
                        Type.WaterSupply -> "Водоснабжение"
                        Type.GasSupply -> "Газоснабжение"
                        Type.ElectricitySupply -> "Электроснабжение"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    account: GetAccounts.AccountInfo,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ ЧЕКБОКС
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Номер счёта
                Text(
                    text = "🆔 ${account.accountNumber}",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Адрес
                account.address?.let { address ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "📍 $address",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Регион
                account.regionId?.let { regionId ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🌍 Регион: $regionId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentStep: Int,
    totalSteps: Int,
    canGoBack: Boolean,
    canGoNext: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    authType: AuthType? = null  // ✅ ДОБАВИТЬ
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoBack) {
                TextButton(
                    onClick = onBack,
                    enabled = !isLoading
                ) {
                    Text("Назад")
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalSteps) { step ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (step < currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Button(
                onClick = onNext,
                enabled = canGoNext && !isLoading
            ) {
                if (isLoading && currentStep == 3) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        isLoading && currentStep == 3 -> "Сохранение..."
                        currentStep == 3 -> "Добавить"
                        currentStep == 2 && authType == AuthType.LOGIN_PASSWORD -> "Войти"
                        else -> "Далее"
                    }
                )
            }
        }
    }
}
