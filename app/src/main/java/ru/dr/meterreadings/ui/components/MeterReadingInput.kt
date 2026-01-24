package ru.dr.meterreadings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // Проверка: передавали ли в этом месяце?
    val wasSubmittedThisMonth = remember(meter.lastSubmissionDate) {
        meter.lastSubmissionDate?.let { dateString ->
            try {
                // Парсим ISO "2026-01-23T00:00:00" или "2026-01-23T14:30:00"
                val datePart = dateString.substringBefore("T")
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val now = Calendar.getInstance()
                    val currentMonth = now.get(Calendar.MONTH) + 1
                    val currentYear = now.get(Calendar.YEAR)

                    // Добавим отладочный вывод
                    println("DEBUG: dateString=$dateString, year=$year, month=$month, currentYear=$currentYear, currentMonth=$currentMonth")

                    year == currentYear && month == currentMonth
                } else {
                    println("DEBUG: parts.size != 3, parts=$parts")
                    false
                }
            } catch (e: Exception) {
                println("DEBUG: Exception parsing date: ${e.message}")
                false
            }
        } ?: false
    }

    // Форматирование даты: "январь 2026" (именительный падеж)
    val formattedMonthYear = remember(meter.lastSubmissionDate) {
        meter.lastSubmissionDate?.let { dateString ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                // LLLL - standalone месяц в именительном падеже (январь), MMMM - родительный (января)
                val outputFormat = SimpleDateFormat("LLLL yyyy", Locale("ru"))
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: ""
            } catch (e: Exception) {
                println("DEBUG: Exception formatting date: ${e.message}")
                ""
            }
        } ?: ""
    }

    val inputInt = remember(inputValue) { inputValue.toIntOrNull() }
    val hasError = remember(inputInt, meter.lastValue) {
        inputInt?.let { newVal ->
            meter.lastValue?.let { last -> newVal <= last } ?: false
        } ?: false
    }

    val canSubmit = remember(inputInt, hasError, isSubmitting) {
        inputInt != null && !hasError && !isSubmitting
    }

    // Цвета карточки: ЗЕЛЁНЫЙ если передано, РОЗОВЫЙ если НЕ передано
    val containerColor = if (wasSubmittedThisMonth) {
        MaterialTheme.colorScheme.primaryContainer  // Светло-зелёный для переданных
    } else {
        MaterialTheme.colorScheme.errorContainer  // Розовый для НЕ переданных
    }

    val onContainerColor = if (wasSubmittedThisMonth) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ВЕРХНЯЯ СТРОКА
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // ЛЕВЫЙ ВЕРХНИЙ УГОЛ
                Column {
                    Text(
                        text = meter.type,
                        style = MaterialTheme.typography.titleSmall,
                        color = onContainerColor
                    )
                    Text(
                        text = "№ ${meter.serialNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.7f)
                    )
                    // Статус с иконкой
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (wasSubmittedThisMonth) Icons.Default.Check else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (wasSubmittedThisMonth) Color(0xFF00C853) else Color(0xFFD50000),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (wasSubmittedThisMonth) "передано" else "не передано",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor
                        )
                    }
                }

                // ПРАВЫЙ ВЕРХНИЙ УГОЛ
                Column(horizontalAlignment = Alignment.End) {
                    // Дата
                    if (formattedMonthYear.isNotEmpty()) {
                        Text(
                            text = formattedMonthYear.replaceFirstChar { it.uppercase() }, // Первая буква заглавная
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                    // Предыдущие показания
                    meter.lastValue?.let { last ->
                        Text(
                            text = "Пред: $last",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                    // Расход
                    meter.lastMonthConsumption?.let { consumption ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Расход: $consumption",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ПОЛЕ ВВОДА + КНОПКА
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Новое показание") },
                    placeholder = { Text("Введите") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),  // Убрали .height(56.dp)
                    isError = hasError,
                    enabled = !isSubmitting,
                    supportingText = if (hasError) {
                        {
                            meter.lastValue?.let { last ->
                                Text(
                                    "Должно быть больше $last",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Button(
                    onClick = {
                        inputInt?.let { value ->
                            onSubmit(value)
                            inputValue = ""
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .height(56.dp)  // Высота только у кнопки
                        .border(
                            width = 2.dp,
                            color = if (canSubmit) Color(0xFF424242) else Color(0xFFBDBDBD),
                            shape = RoundedCornerShape(50)
                        ),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9E9E9E),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE0E0E0),
                        disabledContentColor = Color(0xFF9E9E9E)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Передать")
                    }
                }
            }
        }
    }
}
