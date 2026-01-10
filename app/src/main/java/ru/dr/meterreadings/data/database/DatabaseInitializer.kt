package ru.dr.meterreadings.data.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    fun initializeProviders() {
        CoroutineScope(Dispatchers.IO).launch {
            val existingProviders = providerRepository.getAllProviders().first()

            if (existingProviders.isEmpty()) {
                println("✅ [DatabaseInitializer] Добавляем провайдеров...")

                // КВЦ Нижегородская область
                providerRepository.addProvider(
                    ProviderDomainModel(
                        id = ProviderIds.KVC.toString(),
                        name = "КВЦ",
                        type = Type.WaterSupply,
                        authType = AuthType.ACCOUNT_NUMBER,
                        baseUrl = "https://send.kvc-nn.ru"
                    )
                )

                // В будущем добавите других провайдеров:
                // providerRepository.insertProvider(
                //     ProviderDomainModel(
                //         id = ProviderIds.TNS_ENERGO.toString(),
                //         name = "ТНС Энерго",
                //         type = Type.ElectricitySupply,
                //         authType = AuthType.PASSWORD,
                //         url = "https://www.tns-e.ru"
                //     )
                // )

                println("✅ [DatabaseInitializer] Провайдеры добавлены")
            } else {
                println("ℹ️ [DatabaseInitializer] Провайдеры уже есть (${existingProviders.size})")
            }
        }
    }
}
