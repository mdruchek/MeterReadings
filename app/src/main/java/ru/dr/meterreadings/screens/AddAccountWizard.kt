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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.domain.Type
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.ui.components.ErrorDialog
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

    val searchedAddress by viewModel.searchedAddress.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val shouldResetToStep1 by viewModel.shouldResetToStep1.collectAsStateWithLifecycle()

    val isCreating by viewModel.isCreating.collectAsStateWithLifecycle()
    val createdAccountId by viewModel.createdAccountId.collectAsStateWithLifecycle()

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
                        val hasAccountNumber = accountNumber.isNotBlank()
                        val hasRegionIfNeeded = !providerHasRegions || selectedRegionId != null
                        hasAccountNumber && hasRegionIfNeeded
                    }
                    3 -> searchedAddress != null && !isCreating
                    else -> false
                },
                onNext = {
                    when(currentStep) {
                        1 -> {
                            if (selectedProviderId != null) {
                                viewModel.loadRegionsForProvider(selectedProviderId!!)
                                currentStep = 2
                            }
                        }
                        2 -> {
                            if (selectedProviderId != null && accountNumber.isNotBlank()) {
                                viewModel.searchAccountAddress(
                                    providerId = selectedProviderId!!,
                                    accountNumber = accountNumber,
                                    regionId = selectedRegionId
                                )
                                currentStep = 3
                            }
                        }
                        3 -> {
                            if (selectedProviderId != null && accountNumber.isNotBlank()) {
                                viewModel.createAccount(
                                    profileId = profile.id,
                                    accountNumber = accountNumber
                                )
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
                isLoading = isCreating
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
                    } else {
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
                    }
                }

                // ============================================
                // ШАГ 3: Подтверждение
                // ============================================
                3 -> {
                    item {
                        when {
                            isSearching -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator()
                                        Text("Поиск абонента...")
                                    }
                                }
                            }
                            searchedAddress != null -> {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = "Абонент найден!",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "📍 $searchedAddress",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "🆔 Л/С: $accountNumber",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
fun BottomNavigationBar(
    currentStep: Int,
    totalSteps: Int,
    canGoBack: Boolean,
    canGoNext: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
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
                        else -> "Далее"
                    }
                )
            }
        }
    }
}
