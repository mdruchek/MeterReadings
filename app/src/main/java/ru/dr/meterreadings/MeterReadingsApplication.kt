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
import ru.dr.meterreadings.data.database.DatabaseInitializer
import ru.dr.meterreadings.data.repository.AppSettingsRepository
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.data.util.LogFileManager
import ru.dr.meterreadings.workers.WorkerManager
import javax.inject.Inject

@HiltAndroidApp
class MeterReadingsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var providerRepository: ProviderRepository

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workerManager: WorkerManager

    @Inject
    lateinit var logFileManager: LogFileManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        println("🚀 [Application] Запуск приложения")

        applicationScope.launch {
            try {
                // ✅ Загружаем настройки логирования
                val settings = appSettingsRepository.getSettings().first()

                // ✅ Применяем настройки логирования
                logFileManager.setLoggingEnabled(settings.loggingEnabled)

                // ✅ Очищаем старые логи при запуске
                if (settings.logRetentionDays > 0) {
                    logFileManager.clearOldLogs(settings.logRetentionDays)
                }

                // ✅ Логируем запуск приложения
                logFileManager.log("Application", "📱 Приложение запущено")
                val versionName = try {
                    packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: Exception) {
                    "unknown"
                }

                logFileManager.log("Application", "Версия: $versionName")
                logFileManager.log("Application", "Логирование: ${if (settings.loggingEnabled) "включено" else "отключено"}")
                if (settings.logRetentionDays > 0) {
                    logFileManager.log("Application", "Период хранения логов: ${settings.logRetentionDays} дней")
                }

                // ✅ Инициализируем провайдеров
                logFileManager.log("Application", "🔧 Инициализация провайдеров...")
                databaseInitializer.initializeProviders()
                logFileManager.log("Application", "✅ Провайдеры инициализированы")

                // ✅ Инициализируем Workers
                logFileManager.log("Application", "🔧 Инициализация Workers...")
                workerManager.initializeWorkers()
                logFileManager.log("Application", "✅ Workers инициализированы")

                println("✅ [Application] Инициализация завершена")
                logFileManager.log("Application", "✅ Инициализация приложения завершена")

            } catch (e: Exception) {
                println("❌ [Application] Ошибка инициализации: ${e.message}")
                logFileManager.logError("Application", "Ошибка инициализации приложения", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
