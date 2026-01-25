// app/src/main/java/ru/dr/meterreadings/MeterReadingsApplication.kt

package ru.dr.meterreadings

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.database.DatabaseInitializer  // ✅ ДОБАВИТЬ
import ru.dr.meterreadings.data.repository.AppSettingsRepository
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.ProviderRepository  // ✅ ДОБАВИТЬ
import ru.dr.meterreadings.workers.WorkerManager
import javax.inject.Inject

@HiltAndroidApp
class MeterReadingsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer  // ✅ INJECT DatabaseInitializer

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var providerRepository: ProviderRepository  // ✅ INJECT ProviderRepository

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workerManager: WorkerManager  // ✅ ДОБАВИТЬ

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        println("🚀 [Application] Запуск приложения")

        applicationScope.launch {
            // ✅ ИСПОЛЬЗУЕМ DatabaseInitializer вместо ручной инициализации
            databaseInitializer.initializeProviders()
            workerManager.initializeWorkers()

            println("✅ [Application] Инициализация завершена")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
