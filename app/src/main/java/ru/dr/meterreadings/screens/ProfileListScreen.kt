package ru.dr.meterreadings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ru.dr.meterreadings.models.ui.ProfileUiModel
import ru.dr.meterreadings.viewmodels.ProfileViewModel

/**
 * Главный экран приложения - список профилей пользователя
 *
 * MVVM архитектура:
 * - UI (Composable функции) - отображает данные
 * - State (remember/mutableStateOf) - состояние UI
 * - ViewModel для бизнес-логики и работы с БД
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // =====================================================
    // STATE - состояние экрана (данные + UI состояние)
    // =====================================================

    // Показывать/скрывать диалог создания профиля
    var showDialog by remember { mutableStateOf(false) }

    // Диалог подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<ProfileUiModel?>(null) }

    // Список профилей из БД через ViewModel (с автообновлением!)
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    // Scaffold - каркас Material Design экрана
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профили") }
            )
        },
        // Круглая кнопка плавающего действия
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить профиль")
            }
        }
    ) { paddingValues ->
        // =====================================================
        // CONTENT - основное содержимое экрана
        // =====================================================

        // LazyColumn = RecyclerView в Compose
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // items() - отображение списка из БД
            items(profiles) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = {
                        navController.navigate("profile/${profile.profile.id}")
                    },
                    onEdit = {
                        // TODO: редактирование
                    },
                    onDelete = {
                        profileToDelete = profile
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    // =====================================================
    // ДИАЛОГ СОЗДАНИЯ ПРОФИЛЯ
    // =====================================================
    if (showDialog) {
        ProfileDialog(
            onDismiss = { showDialog = false },
            viewModel = viewModel
        )
    }

    // =====================================================
    // ДИАЛОГ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ
    // =====================================================
    if (showDeleteDialog && profileToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                profileToDelete = null
            },
            title = { Text("Удалить профиль \"${profileToDelete?.profile?.name}\"?") },
            text = { Text("Все данные этого профиля будут удалены. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileToDelete?.let {
                            viewModel.deleteProfile(it.profile.id)
                        }
                        showDeleteDialog = false
                        profileToDelete = null
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
                        showDeleteDialog = false
                        profileToDelete = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Карточка профиля в списке
 *
 * Card - Material Design карточка с тенью и скругленными углами
 */
@Composable
fun ProfileCard(
    profile: ProfileUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        // Занимает всю ширину доступного пространства
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        // Тень карточки
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Внутренний контент карточки
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // =====================================================
            // ИКОНКА ПРОФИЛЯ (эмодзи)
            // =====================================================
            Text(
                text = profile.profile.icon ?: "🏠",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )

            // =====================================================
            // ИНФОРМАЦИЯ О ПРОФИЛЕ
            // =====================================================
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Название профиля
                    Text(
                        text = profile.profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Статистика профиля
                Text(
                    text = "${profile.addressCount} адресов • ${profile.accountCount} счетов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Дата последнего обновления
                profile.lastUpdateDate?.let { date ->
                    Text(
                        text = "Обновлено: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // =====================================================
            // МЕНЮ С ТРЕМЯ ТОЧКАМИ
            // =====================================================
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Редактировать") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )

                    Divider()

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
 * Диалог создания нового профиля
 *
 * AlertDialog - стандартный Material 3 диалог
 */
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel
) {
    // =====================================================
    // STATE ДИАЛОГА - локальное состояние
    // =====================================================

    // Название профиля
    var profileName by remember { mutableStateOf("") }

    // Выбранная иконка
    var selectedIcon by remember { mutableStateOf("🏠") }

    AlertDialog(
        // Закрыть диалог при клике вне его
        onDismissRequest = onDismiss,

        // Заголовок диалога
        title = { Text("Новый профиль") },

        // Контент диалога
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // =====================================================
                // ПОЛЕ ВВОДА НАЗВАНИЯ
                // =====================================================
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Название профиля") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // =====================================================
                // ВЫБОР ИКОНКИ
                // =====================================================
                IconPicker(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )
            }
        },

        // Кнопка подтверждения
        confirmButton = {
            TextButton(
                onClick = {
                    val name = profileName.trim()
                    if (name.isNotEmpty()) {
                        viewModel.createProfile(name = name, icon = selectedIcon)
                        onDismiss()
                    }
                }
            ) {
                Text("Создать")
            }
        },

        // Кнопка отмены
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Компонент выбора иконки (эмодзи)
 */
@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit
) {
    Column {
        // Заголовок секции
        Text(
            text = "Иконка",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Горизонтальный ряд эмодзи
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Список доступных иконок
            val icons = listOf("🏠", "🏢", "🏪", "🏭", "🏘️", "🏡", "📍", "📋")

            icons.forEach { icon ->
                val isSelected = selectedIcon == icon

                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onIconSelected(icon) }
                )
            }
        }
    }
}
