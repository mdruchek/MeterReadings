@file:OptIn(ExperimentalMaterial3Api::class)

package ru.dr.meterreadings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import ru.dr.meterreadings.models.domain.AccountDomainModel
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.ui.AccountUiModel
import ru.dr.meterreadings.models.ui.ProviderUiModel

/**
 * Пошаговый мастер добавления компании (лицевого счета)
 *
 * Шаги:
 * 1. Выбор компании из списка
 * 2. Ввод лицевого счета
 * 3. Загрузка данных + подтверждение
 */
@Composable
            fun AddAccountWizard(
    profile: ProfileDomainModel,
    onAccountAdded: (AccountDomainModel) -> Unit,   // callback при успехе
    onCancel: () -> Unit                 // закрыть wizard
) {
    // =====================================================
    // STATE - состояние wizard
    // =====================================================
    var currentStep by remember { mutableStateOf(1) }  // 1,2,3
    var searchQuery by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    // Тестовые компании (позже из БД/config)
    val allProviders = remember {
        listOf(
            ProviderUiModel("mosvodokanal", "🏠 Мосводоканал", "Вода", ""),
            ProviderUiModel("mosenergosbyt", "⚡ Мосэнергосбыт", "Электричество", ""),
            ProviderUiModel("mosoblgaz", "🔥 Мособлгаз", "Газ", ""),
            ProviderUiModel("podmoskovye", "🌡️ Подмосковная электросеть", "Электричество","")
        )
    }

    // Фильтрация по поиску
    val filteredProviders = allProviders.filter { provider ->
        provider.name.contains(searchQuery, ignoreCase = true) ||
                provider.type.contains(searchQuery, ignoreCase = true)
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
                // =====================================================
                // ШАГ 1: ВЫБОР КОМПАНИИ
                // =====================================================
                1 -> {
                    item {
                        // Поиск
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Поиск компании") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // Список компаний
                    items(filteredProviders) { provider ->
                        Provid  erCard(
                            provider = provider,
                            isSelected = false,  // позже добавим выбор
                            onClick = {
                                // TODO: Сохранить выбор и перейти к шагу 2
                                currentStep = 2
                            }
                        )
                    }
                }

                // =====================================================
                // ШАГ 2: ЛИЦЕВОЙ СЧЕТ
                // =====================================================
                2 -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // TODO: Показать выбранную компанию
                            Text(
                                text = "🏠 Мосводоканал",  // из шага 1
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

                // =====================================================
                // ШАГ 3: ПОДТВЕРЖДЕНИЕ
                // =====================================================
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

        // Нижняя панель с кнопками навигации
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
                        // TODO: Загрузить данные с API
                        val newAccount = AccountUiModel(
                            account=AccountDomainModel(
                                id = System.currentTimeMillis().toString(),
                                profileId = profile.id,
                                providerId = "mosvodokanal",
                                accountNumber = accountNumber,
                                address = "ул. Ленина, д. 5, кв. 12"
                            )
                        )
                        onAccountAdded(newAccount)
                        onCancel()
                    }
                }
            },
            onBack = { if (currentStep > 1) currentStep-- }
        )
    }
}

/**
 * Карточка провайдера (компании)
 */
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
                text = provider.logoURL,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = provider.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Нижняя панель с кнопками и прогрессом
 */
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
