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
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.domain.Type
import ru.dr.meterreadings.models.ui.AccountUiModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import ru.dr.meterreadings.viewmodels.AddAccountViewModel

@Composable
fun AddAccountWizard(
    profile: ProfileDomainModel,
    onAccountAdded: (AccountUiModel) -> Unit,
    onCancel: () -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel()
) {
    // =====================================================
    // STATE
    // =====================================================

    var currentStep by remember { mutableStateOf(1) }
    var accountNumber by remember { mutableStateOf("") }

    // State из ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredProviders by viewModel.filteredProviders.collectAsStateWithLifecycle()
    val selectedProviderId by viewModel.selectedProviderId.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.getSelectedProvider().collectAsStateWithLifecycle()

    val providerHasRegions by viewModel.providerHasRegions.collectAsStateWithLifecycle()
    val regions by viewModel.regions.collectAsStateWithLifecycle()
    val selectedRegionId by viewModel.selectedRegionId.collectAsStateWithLifecycle()

    val searchedAddress by viewModel.searchedAddress.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

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
                    3 -> searchedAddress != null
                    else -> false
                },
                onNext = {
                    when(currentStep) {
                        1 -> currentStep = 2
                        2 -> {
                            // Поиск адреса
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
                            // Сохранение
                            if (selectedProviderId != null && searchedAddress != null) {
                                val newAccount = AccountUiModel(
                                    account = AccountDomainModel(
                                        id = System.currentTimeMillis().toString(),
                                        profileId = profile.id,
                                        providerId = selectedProviderId!!,
                                        accountNumber = accountNumber,
                                        regionId = selectedRegionId?.toIntOrNull()
                                    ),
                                    address = searchedAddress
                                )
                                onAccountAdded(newAccount)
                            }
                        }
                    }
                },
                onBack = { if (currentStep > 1) currentStep-- }
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

                    // Выбор региона (если провайдер имеет регионы)
                    if (providerHasRegions && regions.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Регион",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(regions) { region ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectRegion(region.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (region.id == selectedRegionId)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = region.name,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    // Ввод лицевого счёта
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

                // ============================================
                // ШАГ 3: Подтверждение
                // ============================================
                3 -> {
                    item {
                        if (isSearching) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (searchedAddress != null) {
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
                                        text = "📍 $searchedAddress\n" +
                                                "🆔 Л/С: $accountNumber",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "❌ Абонент не найден",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
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
                text = "🏢",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.provider.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = provider.provider.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onBack: () -> Unit
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
                TextButton(onClick = onBack) {
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

            TextButton(
                onClick = onNext,
                enabled = canGoNext
            ) {
                Text(
                    text = when(currentStep) {
                        3 -> "Добавить"
                        else -> "Далее"
                    }
                )
            }
        }
    }
}
