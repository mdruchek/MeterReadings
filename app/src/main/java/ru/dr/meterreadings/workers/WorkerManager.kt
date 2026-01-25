// app/src/main/java/ru/dr/meterreadings/workers/WorkerManager.kt

package ru.dr.meterreadings.workers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import ru.dr.meterreadings.data.repository.AppSettingsRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val providerRepository: ProviderRepository
) {

    /**
     * Инициализация/обновление всех Workers
     */
    suspend fun initializeWorkers() {
        try {
            println("⚙️ [WorkerManager] Инициализация Workers")

            val globalSettings = appSettingsRepository.getSettings().first()
            val providers = providerRepository.getAllProviders().first()

            println("🔍 [WorkerManager] Провайдеров в БД: ${providers.size}")

            // ============================================
            // 1. АВТООБНОВЛЕНИЕ ПЕРИОДОВ
            // ============================================

            val providersWithAutoUpdate = providers.filter { it.autoUpdateEnabled }

            if (providersWithAutoUpdate.isNotEmpty()) {
                PeriodUpdateWorker.schedule(
                    context = context,
                    intervalHours = globalSettings.autoUpdateIntervalHours
                )
                println("✅ [WorkerManager] PeriodUpdateWorker запланирован (каждые ${globalSettings.autoUpdateIntervalHours} ч)")
                println("   День старта обновления: ${globalSettings.autoUpdateStartDay}")
                println("   Провайдеры с автообновлением:")
                providersWithAutoUpdate.forEach { provider ->
                    println("    - ${provider.name}")
                }
            } else {
                PeriodUpdateWorker.cancel(context)
                println("⏭️ [WorkerManager] PeriodUpdateWorker отменён (нет провайдеров с автообновлением)")
            }

            // ============================================
            // 2. НАПОМИНАНИЯ
            // ============================================

            if (!globalSettings.globalNotificationsEnabled) {
                MeterReadingNotificationWorker.cancel(context)
                println("⏭️ [WorkerManager] NotificationWorker отменён (глобальные уведомления отключены)")
            } else if (!globalSettings.globalRemindersEnabled) {
                MeterReadingNotificationWorker.cancel(context)
                println("⏭️ [WorkerManager] NotificationWorker отменён (глобальные напоминания отключены)")
            } else {
                val providersWithReminders = providers.filter { it.reminderEnabled }

                if (providersWithReminders.isNotEmpty()) {
                    MeterReadingNotificationWorker.schedule(
                        context = context,
                        hour = globalSettings.reminderTimeHour,
                        minute = globalSettings.reminderTimeMinute
                    )
                    println("✅ [WorkerManager] NotificationWorker запланирован (ежедневно в ${globalSettings.reminderTimeHour}:${String.format("%02d", globalSettings.reminderTimeMinute)})")
                    println("   Режим: ${globalSettings.reminderPeriodMode}")

                    if (globalSettings.reminderPeriodMode == "AUTO") {
                        println("   Напоминать за ${globalSettings.reminderDaysBeforeStart} дней до периода")
                    }

                    println("   Провайдеры с напоминаниями:")
                    providersWithReminders.forEach { provider ->
                        if (globalSettings.reminderPeriodMode == "MANUAL" && provider.reminderCustomStartDay != null) {
                            println("    - ${provider.name}: с ${provider.reminderCustomStartDay} числа (ручной режим)")
                        } else {
                            println("    - ${provider.name}: автоматически")
                        }
                    }
                } else {
                    MeterReadingNotificationWorker.cancel(context)
                    println("⏭️ [WorkerManager] NotificationWorker отменён (нет провайдеров с напоминаниями)")
                }
            }

        } catch (e: Exception) {
            println("❌ [WorkerManager] Ошибка инициализации Workers: ${e.message}")
            e.printStackTrace()
        }
    }
}
