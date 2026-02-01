// app/src/main/java/ru/dr/meterreadings/data/database/DatabaseInitializer.kt

package ru.dr.meterreadings.data.database

import kotlinx.coroutines.flow.first
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.domain.constants.ProviderIds
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val providerRepository: ProviderRepository
) {

    suspend fun initializeProviders() {
        val existingProviders = providerRepository.getAllProviders().first()
        println("🔍 [DatabaseInitializer] Провайдеров в БД: ${existingProviders.size}")

        existingProviders.forEach { provider ->
            println("   📋 ID=${provider.id}, Name=${provider.name}")
        }

        if (existingProviders.isEmpty()) {
            println("✅ [DatabaseInitializer] Добавляем провайдеров...")

            // КВЦ Нижегородская область
            providerRepository.addProvider(
                ProviderDomainModel(
                    id = ProviderIds.KVC,
                    name = "КВЦ",
                    type = Type.WaterSupply,
                    authType = AuthType.ACCOUNT_NUMBER,
                    baseUrl = "https://send.kvc-nn.ru",
                    logoUrl = null,

                    // ============================================
                    // ПЕРИОД ПЕРЕДАЧИ (загрузится из API)
                    // ============================================
                    transmissionPeriodStartDay = null,
                    transmissionPeriodEndDay = null,
                    lastPeriodUpdate = null,
                    periodLoadedForMonth = null,

                    // ============================================
                    // АВТООБНОВЛЕНИЕ
                    // ============================================
                    autoUpdateEnabled = true,

                    // ============================================
                    // УВЕДОМЛЕНИЯ
                    // ============================================
                    notificationsEnabled = true,

                    // ============================================
                    // НАПОМИНАНИЯ
                    // ============================================
                    reminderEnabled = true,
                    reminderCustomStartDay = null,      // ✨ НОВОЕ
                )
            )

            providerRepository.addProvider(
                ProviderDomainModel(
                    id = ProviderIds.TNS,
                    name = "ТНС Энерго",
                    type = Type.ElectricitySupply,
                    authType = AuthType.LOGIN_PASSWORD,
                    baseUrl = "https://mobile-api-rostov.tns-e.ru",
                    logoUrl = null,
                    transmissionPeriodStartDay = null,
                    transmissionPeriodEndDay = null,
                    lastPeriodUpdate = null,
                    periodLoadedForMonth = null,
                    autoUpdateEnabled = true,
                    notificationsEnabled = true,
                    reminderEnabled = true,
                    reminderCustomStartDay = null
                )
            )

            println("✅ [DatabaseInitializer] Провайдеры добавлены")
        } else {
            println("ℹ️ [DatabaseInitializer] Провайдеры уже есть (${existingProviders.size})")
        }
    }
}
