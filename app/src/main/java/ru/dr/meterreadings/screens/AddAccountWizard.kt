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
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.domain.ProviderDomainModel
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

    // Локальное состояние мастера
    var currentStep by remember { mutableStateOf(1) }
    var accountNumber by remember { mutableStateOf("") }

    // ✅ НОВОЕ - состояние из ViewModel (провайдеры из БД)
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredProviders by viewModel.filteredProviders.collectAsStateWithLifecycle()
    val selectedProviderId by viewModel.selectedProviderId.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.getSelectedProvider().collectAsStateWithLifecycle()

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
        // ← ВОТ СЮДА переносим bottomBar!
        bottomBar = {
            BottomNavigationBar(
                currentStep = currentStep,
                totalSteps = 3,
                canGoBack = currentStep > 1,
                accountNumberValid = accountNumber.isNotBlank(),
                onNext = {
                    when(currentStep) {
                        1 -> currentStep = 2
                        2 -> if (accountNumber.isNotBlank()) currentStep = 3
                        3 -> {
                            // ✅ НОВОЕ - проверяем что провайдер выбран
                            if (selectedProviderId == null) {
                                println("❌ Провайдер не выбран!")
                                return@BottomNavigationBar
                            }

                            val newAccount = AccountUiModel(
                                account = AccountDomainModel(
                                    id = System.currentTimeMillis().toString(),
                                    profileId = profile.id,
                                    providerId = selectedProviderId!!,
                                    accountNumber = accountNumber
                                ),
                                address = "ул. Ленина, д. 5, кв. 12"
                            )
                            onAccountAdded(newAccount)
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
                .padding(paddingValues)  // ← Это важно!
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when(currentStep) {
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
                                currentStep = 2
                            }
                        )
                    }
                }

                2 -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val providerName = selectedProvider?.name ?: "Провайдер"
                            // Иконка по типу провайдера (используем enum!)
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
                                text = "Введите лицевой счет",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("Лицевой счет") },
                            placeholder = { Text("123456789") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                3 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Данные успешно загружены!",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "🏠 Мосводоканал\n" +
                                            "л/с 123456789\n\n" +
                                            "📍 ул. Ленина, д. 5, кв. 12\n" +
                                            "👤 Иванов И.И.\n\n" +
                                            "✅ Загружено 2 счетчика\n" +
                                            "💧 Холодная вода\n" +
                                            "🔥 Горячая вода",
                                    style = MaterialTheme.typography.bodyMedium
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
                text = "http://",
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
    accountNumberValid: Boolean,
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
            // Кнопка "Назад"
            if (canGoBack) {
                TextButton(onClick = onBack) {
                    Text("Назад")
                }
            } else {
                Spacer(Modifier.width(48.dp))  // ← Чтобы выравнивание не прыгало
            }

            // Прогресс шагов
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

            // Кнопка "Далее"
            val nextEnabled = when(currentStep) {
                2 -> accountNumberValid
                else -> true
            }
            TextButton(
                onClick = onNext,
                enabled = nextEnabled
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
