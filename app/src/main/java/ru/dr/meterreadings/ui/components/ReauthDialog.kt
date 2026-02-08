package ru.dr.meterreadings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.dr.meterreadings.models.ui.AuthError

/**
 * Диалог повторной авторизации
 *
 * Показывается когда токен истёк на ProfileDetailScreen.
 */
@Composable
fun ReauthDialog(
    authError: AuthError,
    onDismiss: () -> Unit,
    onReauth: (login: String, password: String) -> Unit
) {
    var login by remember {
        mutableStateOf(
            when (authError) {
                is AuthError.TokenExpiredNoRefresh -> authError.login
                is AuthError.RefreshFailed -> authError.login
                else -> ""
            }
        )
    }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(authError.title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(authError.message)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ✅ Дополнительная информация для разработчика
                if (authError is AuthError.TokenExpiredNoRefresh) {
                    Text(
                        text = "ℹ️ Для разработчика:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Реализуйте TnsRepository.refreshAccessToken() для автоматического обновления.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Форма входа
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Email") },
                    placeholder = { Text("example@mail.ru") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (login.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        onReauth(login, password)
                    }
                },
                enabled = login.isNotBlank() && password.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isLoading) "Вход..." else "Войти")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Отмена")
            }
        }
    )
}
