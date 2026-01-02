package ru.dr.meterreadings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint  // ← HILT!
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.viewmodels.ProfileViewModel

/**
 * Экран деталей профиля
 *
 * Показывает:
 * - Информацию о профиле
 * - Список адресов (группировка)
 * - Компании по адресам
 * - Кнопку "Добавить компанию"
 * - Кнопку "Передать все показания"
 */
@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profileId: String,
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()  // ← ДОБАВЛЕНО!
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали профиля") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ProfileHeader(profileId)
            Spacer(modifier = Modifier.height(24.dp))

            val profile by viewModel.profile.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                viewModel.loadProfile(profileId)
            }

            profile?.let { profile: ProfileDomainModel ->
                ProfileStats(profile)
            }?: Text(
                "Загрузка профиля...",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))
            AddressSection()
            Spacer(modifier = Modifier.height(24.dp))
            ActionButtons(navController, profileId)
        }
    }
}

/**
 * Заголовок профиля с названием и иконкой
 */
@Composable
private fun ProfileHeader(profileId: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("ID: $profileId", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/**
 * Статистика профиля (кол-во компаний, адресов, показаний)
 */
@Composable
private fun ProfileStats(profile: ProfileDomainModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileStatCard("🏠", "Адресов", "3")
        ProfileStatCard("🏢", "Компаний", "5")
        ProfileStatCard("📊", "Показаний", "12")
    }
}

@Composable
private fun ProfileStatCard(icon: String, label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label)
        }
    }
}

/**
 * Секция адресов (группировка компаний по адресам)
 */
@Composable
private fun AddressSection() {
    Column {
        Text(
            text = "Адреса и компании",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Заглушка адреса
        AddressGroupCard(
            address = "ул. Ленина, д. 5, кв. 12",
            companiesCount = 3
        )
        AddressGroupCard(
            address = "пос. Лесной, д. 8",
            companiesCount = 2
        )
    }
}

@Composable
private fun AddressGroupCard(
    address: String,
    companiesCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = address,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$companiesCount компаний",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Кнопки действий внизу экрана
 */
@Composable
private fun ActionButtons(navController: NavHostController, profileId: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { /* TODO */ }) { Text("Редактировать") }
        Button(onClick = { navController.navigate("addresses/$profileId") }) {
            Text("Адреса")
        }
    }
}
