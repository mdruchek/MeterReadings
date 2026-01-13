// app/src/main/java/ru/dr/meterreadings/ui/components/MeterReadingInput.kt

package ru.dr.meterreadings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import ru.dr.meterreadings.models.ui.MeterUiModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Карточка для ввода показаний счётчика
 *
 * Визуальные индикаторы:
 * - 🟢 Зелёный фон: показания переданы в этом месяце
 * - 🔴 Красный фон: показания НЕ переданы в этом месяце
 *
 * @param meter - данные счётчика
 * @param onSubmit - callback при нажатии кнопки отправки
 * @param isSubmitting - флаг отправки (показывает прогресс)
 */
@Composable
fun MeterReadingInput(
    meter: MeterUiModel,
    onSubmit: (Int) -> Unit,
    isSubmitting: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Состояние поля ввода
    var inputValue by remember { mutableStateOf("") }

    // ============================================
    // ПРОВЕРКА: передавали ли в этом месяце?
    // ============================================
    val wasSubmittedThisMonth = remember(meter.lastUpdateDate) {
        meter.lastUpdateDate?.let {
            val lastUpdate = Calendar.getInstance().apply {
                timeInMillis = it
            }
            val now = Calendar.getInstance()

            // Сравниваем год и месяц
            lastUpdate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    lastUpdate.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        } ?: false  // Если даты нет - считаем не передавали
    }

    // ============================================
    // ФОРМАТИРОВАНИЕ ДАТЫ
    // ============================================
    val formattedDate = remember(meter.lastUpdateDate) {
        meter.lastUpdateDate?.let {
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
            dateFormat.format(Date(it))
        } ?: "Никогда"
    }

    // ============================================
    // ВАЛИДАЦИЯ ВВОДА
    // ============================================
    val inputInt = remember(inputValue) {
        inputValue.toIntOrNull()
    }

    val hasError = remember(inputInt, meter.lastValue) {
        inputInt?.let { newVal ->
            meter.lastValue?.let { last -> newVal < last } ?: false
        } ?: false
    }

    val canSubmit = remember(inputInt, hasError, isSubmitting) {
        inputInt != null && !hasError && !isSubmitting
    }

    // ============================================
    // UI
    // ============================================
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // 🟢/🔴 Цветной фон в зависимости от статуса
            containerColor = if (wasSubmittedThisMonth) {
                MaterialTheme.colorScheme.tertiaryContainer  // 🟢 Зелёный
            } else {
                MaterialTheme.colorScheme.errorContainer     // 🔴 Красный
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // ========================================
            // СТРОКА 1: Тип и последнее показание
            // ========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть: тип и номер
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meter.type,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (wasSubmittedThisMonth) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = "№ ${meter.serialNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wasSubmittedThisMonth) {
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        }
                    )
                }

                // Правая часть: последнее показание
                meter.lastValue?.let { last ->
                    Text(
                        text = "Пред: $last",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wasSubmittedThisMonth) {
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ========================================
            // СТРОКА 2: Поле ввода + Кнопка
            // ========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Поле ввода
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Новое показание") },
                    placeholder = { Text("Введите") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    isError = hasError,
                    enabled = !isSubmitting,
                    supportingText = {
                        if (hasError) {
                            meter.lastValue?.let { last ->
                                Text(
                                    "Меньше предыдущего ($last)",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Кнопка отправки
                Button(
                    onClick = {
                        inputInt?.let { value ->
                            onSubmit(value)
                            inputValue = ""  // Очищаем после отправки
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("→")
                    }
                }
            }

            // ========================================
            // СТРОКА 3: Статус + Дата
            // ========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть: индикатор статуса
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (wasSubmittedThisMonth) "✓" else "⚠",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (wasSubmittedThisMonth) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = if (wasSubmittedThisMonth) "Передано" else "Не передано",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wasSubmittedThisMonth) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }

                // Правая часть: дата
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (wasSubmittedThisMonth) {
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}
