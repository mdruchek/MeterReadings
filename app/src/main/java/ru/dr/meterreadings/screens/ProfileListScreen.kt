package ru.dr.meterreadings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ru.dr.meterreadings.models.domain.ProfileDomainModel


/**
 * Главный экран приложения - список профилей пользователя
 *
 * MVVM архитектура:
 * - UI (Composable функции) - отображает данные
 * - State (remember/mutableStateOf) - состояние UI
 * - Позже добавим ViewModel для бизнес-логики
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    navController: NavHostController
) {
    // =====================================================
    // STATE - состояние экрана (данные + UI состояние)
    // =====================================================

    // Показывать/скрывать диалог создания профиля
    // mutableStateOf - "реактивная переменная", изменения автоматически
    // перерисовывают UI только там, где она используется
    var showDialog by remember { mutableStateOf(false) }

    // Список профилей - пока в памяти, позже из БД через ViewModel
    // mutableStateListOf - специальный список, который уведомляет UI об изменениях
    // remember - сохраняет список между перерисовками экрана
    val profiles = remember {
        mutableStateListOf(
            ProfileDomainModel(id = "1", name = "Моя недвижимость", icon = "🏠"),
            ProfileDomainModel(id = "2", name = "Служебная недвижимость", icon = "🏢")
        )
    }

    // Scaffold - каркас Material Design экрана
    // Автоматически предоставляет:
    // - topBar (верхняя панель)
    // - floatingActionButton (круглая кнопка)
    // - content (основное содержимое с отступами)
    Scaffold(
        topBar = {
            // TopAppBar - стандартная верхняя панель Material 3
            TopAppBar(
                title = {
                    // Text внутри TopAppBar автоматически стилизуется
                    Text("Передача показаний")
                }
            )
        },
        // Круглая кнопка плавающего действия - главное действие экрана
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showDialog = true  // ← Открываем диалог создания профиля
                    //navController.navigate("add_account/${profile.id}")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить профиль")
                //Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { paddingValues ->
        // =====================================================
        // CONTENT - основное содержимое экрана
        // =====================================================

        // LazyColumn = RecyclerView в Compose
        // Эффективно: создает только видимые элементы, остальные лениво
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()                    // 1. Заполнить экран
                .padding(16.dp),                  // 2. Отступы 16dp
            contentPadding = paddingValues,       // 3. Отступы от topBar/FAB
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // items() - стандартный способ отображения списка
            // Для каждого элемента из profiles вызывается ProfileCard
            items(profiles) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = {
                        navController.navigate("profile/${profile.id}")
                    }
                )
            }
        }

        // =====================================================
        // ДИАЛОГ - показывается поверх основного контента
        // =====================================================
        // if (условие) - условный рендеринг в Compose
        if (showDialog) {
            // ProfileDialog - модальное окно поверх экрана
            ProfileDialog(
                // Функции обратного вызова (callbacks)
                onDismiss = { showDialog = false },  // закрыть диалог
                onProfileCreated = { newProfile ->
                    // Добавляем новый профиль в список
                    // Изменение mutableStateListOf автоматически перерисовывает UI
                    profiles.add(newProfile)
                    // Скрываем диалог
                    showDialog = false
                }
            )
        }
    }
}

/**
 * Карточка профиля в списке
 *
 * Card - Material Design карточка с тенью и скругленными углами
 */
@Composable
fun ProfileCard(
    profile: ProfileDomainModel,
    onClick: (() -> Unit)? = null
) {
    Card(
        // Занимает всю ширину доступного пространства
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()  // ← Безопасный вызов
            },
        // Тень карточки (elevation)
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Внутренний контент карточки
        Row(
            // Отступы внутри карточки
            modifier = Modifier.padding(16.dp),
            // Вертикальное выравнивание элементов строки
            verticalAlignment = Alignment.CenterVertically
        ) {
            // =====================================================
            // ИКОНКА ПРОФИЛЯ (эмодзи)
            // =====================================================
            Text(
                // Текст эмодзи (из модели Profile)
                text = profile.icon ?: "📋",  // ?: - Elvis operator (null coalescing)
                // Большой шрифт для эмодзи
                style = MaterialTheme.typography.headlineLarge,
                // Отступ справа от иконки
                modifier = Modifier.padding(end = 16.dp)
            )

            // =====================================================
            // ИНФОРМАЦИЯ О ПРОФИЛЕ
            // =====================================================
            // Column с весом 1f - занимает всё оставшееся место
            Column(modifier = Modifier.weight(1f)) {
                // Название профиля - основной текст
                Text(
                    text = profile.name,
                    // Заголовок карточки
                    style = MaterialTheme.typography.titleLarge
                )

                // Пробел между строками
                Spacer(modifier = Modifier.height(4.dp))

                // Заглушка - позже заменим на реальные данные из БД
                Text(
                    text = "0 компаний • 0 адресов",
                    // Меньший шрифт, вторичный цвет
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Диалог создания нового профиля
 *
 * AlertDialog - стандартный Material 3 диалог с заголовком, контентом и кнопками
 */
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,           // закрыть диалог
    onProfileCreated: (ProfileDomainModel) -> Unit  // что делать с новым профилем
) {
    // =====================================================
    // STATE ДИАЛОГА - локальное состояние
    // =====================================================

    // Название профиля - TextFieldValue лучше String для Compose
    // (поддерживает курсор, выделение, историю изменений)
    var profileName by remember { mutableStateOf(TextFieldValue("")) }

    // Выбранная иконка - строка с эмодзи
    var selectedIcon by remember { mutableStateOf("🏠") }

    AlertDialog(
        // Закрыть диалог при клике вне его
        onDismissRequest = onDismiss,

        // Заголовок диалога
        title = { Text("Новый профиль") },

        // Контент диалога
        text = {
            // Вертикальный контейнер с отступами между элементами
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // =====================================================
                // ПОЛЕ ВВОДА НАЗВАНИЯ
                // =====================================================
                OutlinedTextField(
                    // Текущее значение поля
                    value = profileName,
                    // Обработчик изменений - автоматически перерисовывает UI
                    onValueChange = { profileName = it },
                    // Подпись поля (плавает вверх при фокусе)
                    label = { Text("Название профиля") },
                    // Занимает всю ширину диалога
                    modifier = Modifier.fillMaxWidth(),
                    // Одна строка (без переноса)
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

        // Кнопка подтверждения (справа)
        confirmButton = {
            TextButton(
                // Простая проверка - название не пустое
                onClick = {
                    val name = profileName.text.trim()
                    if (name.isNotEmpty()) {
                        // Создаем объект Profile
                        val newProfile = ProfileDomainModel(
                            // Временный ID - позже UUID из БД
                            id = System.currentTimeMillis().toString(),
                            name = name,
                            icon = selectedIcon
                        )
                        // Вызываем callback родителя
                        onProfileCreated(newProfile)
                    }
                }
            ) {
                Text("Создать")
            }
        },

        // Кнопка отмены (слева)
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Компонент выбора иконки (эмодзи)
 *
 * Можно вынести в отдельный файл позже
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Список доступных иконок
            val icons = listOf(
                "🏠", "🏢", "🏪", "🏭", "🏘️", "🏡", "📍", "📋"
            )

            // forEach создает Text для каждой иконки
            icons.forEach { icon ->
                // Проверяем, выбрана ли текущая иконка
                val isSelected = selectedIcon == icon

                Text(
                    text = icon,
                    // Большой размер для эмодзи
                    style = MaterialTheme.typography.headlineLarge,
                    // Цвет зависит от выбора
                    color = if (isSelected)
                    // Основной цвет темы если выбрано
                        MaterialTheme.colorScheme.primary
                    else
                    // Вторичный цвет если не выбрано
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    // Размер области клика
                    modifier = Modifier
                        .size(48.dp)  // квадрат 48x48 dp
                        .clickable {
                            // Обработчик клика - меняем состояние
                            onIconSelected(icon)
                        }
                )
            }
        }
    }
}
