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
import ru.dr.meterreadings.data.repository.ProfileRepository
import ru.dr.meterreadings.data.repository.ProviderRepository  // ✅ ДОБАВИТЬ
import ru.dr.meterreadings.workers.MeterReadingNotificationWorker
import ru.dr.meterreadings.workers.PeriodUpdateWorker
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
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        println("🚀 [Application] Запуск приложения")

        applicationScope.launch {
            // ✅ ИСПОЛЬЗУЕМ DatabaseInitializer вместо ручной инициализации
            databaseInitializer.initializeProviders()
            initializeWorkers()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    private suspend fun initializeWorkers() {
        try {
            println("⚙️ [Application] Инициализация Workers")

            // ✅ ИСПОЛЬЗУЕМ ProviderRepository вместо ProfileRepository
            val providers = providerRepository.getAllProviders().first()
            println("🔍 [Application] Провайдеров в БД: ${providers.size}")

            var shouldSchedulePeriodUpdate = false
            var shouldScheduleReminders = false
            var updateIntervalHours = 6
            var reminderHour = 9
            var reminderMinute = 0

            for (provider in providers) {
                println("📋 [Application] Провайдер: ${provider.name}")
                println("   Автообновление: ${provider.autoUpdateEnabled}")
                println("   Напоминания: ${provider.reminderEnabled}")

                if (provider.autoUpdateEnabled) {
                    shouldSchedulePeriodUpdate = true
                    updateIntervalHours = provider.updateIntervalHours
                    println("   ✅ Автообновление активно, интервал: $updateIntervalHours ч")
                }

                if (provider.reminderEnabled) {
                    shouldScheduleReminders = true
                    reminderHour = provider.reminderTimeHour
                    reminderMinute = provider.reminderTimeMinute
                    println("   ✅ Напоминания активны, время: $reminderHour:$reminderMinute")
                }
            }

            if (shouldSchedulePeriodUpdate) {
                PeriodUpdateWorker.schedule(
                    context = applicationContext,
                    intervalHours = updateIntervalHours
                )
                println("✅ [Application] PeriodUpdateWorker запланирован (каждые $updateIntervalHours ч)")
            } else {
                PeriodUpdateWorker.cancel(applicationContext)
                println("⏭️ [Application] PeriodUpdateWorker отменён")
            }

            if (shouldScheduleReminders) {
                MeterReadingNotificationWorker.schedule(
                    context = applicationContext,
                    hour = reminderHour,
                    minute = reminderMinute
                )
                println("✅ [Application] NotificationWorker запланирован (ежедневно в $reminderHour:$reminderMinute)")
            } else {
                MeterReadingNotificationWorker.cancel(applicationContext)
                println("⏭️ [Application] NotificationWorker отменён")
            }

        } catch (e: Exception) {
            println("❌ [Application] Ошибка инициализации Workers: ${e.message}")
            e.printStackTrace()
        }
    }
}
