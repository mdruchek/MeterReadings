// app/src/main/java/ru/dr/meterreadings/workers/MeterReadingNotificationWorker.kt

package ru.dr.meterreadings.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import ru.dr.meterreadings.data.local.dao.MeterDao
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.AppSettingsRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.data.util.LogFileManager
import ru.dr.meterreadings.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Worker для напоминаний о передаче показаний счётчиков
 *
 * Логика работы:
 * 1. Проверяет, наступил ли период передачи для провайдеров
 * 2. Для провайдеров в периоде:
 *    - Находит счётчики, которые можно передать
 *    - Проверяет, передавались ли показания в этом месяце
 *    - Показывает уведомление, если есть непереданные
 *
 * Запускается ежедневно в настроенное время (например, 09:00)
 */
@HiltWorker
class MeterReadingNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val providerRepository: ProviderRepository,
    private val accountRepository: AccountRepository,
    private val meterDao: MeterDao,
    private val notificationHelper: NotificationHelper,
    private val appSettingsRepository: AppSettingsRepository,
    private val logFileManager: LogFileManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        logFileManager.log(TAG, "🔔 Начинаем проверку напоминаний")
        return try {
            println("🔔 [NotificationWorker] Проверка напоминаний о передаче показаний")

            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

            // ✅ Загружаем глобальные настройки
            val globalSettings = appSettingsRepository.getSettings().first()

            // ============================================
            // ПРОВЕРКА ГЛОБАЛЬНЫХ ФЛАГОВ
            // ============================================

            // ❌ Уровень 0: МАСТЕР-флаг уведомлений
            if (!globalSettings.globalNotificationsEnabled) {
                println("⏭️ [NotificationWorker] Глобальные уведомления отключены")
                return Result.success()
            }

            // ❌ Уровень 1: Глобальные напоминания
            if (!globalSettings.globalRemindersEnabled) {
                println("⏭️ [NotificationWorker] Глобальные напоминания отключены")
                return Result.success()
            }

            // ✅ Загружаем провайдеров
            val allProviders = providerRepository.getAllProviders().first()

            // Фильтруем: только с включёнными напоминаниями
            val providersWithReminders = allProviders.filter { it.reminderEnabled }

            if (providersWithReminders.isEmpty()) {
                println("⏭️ [NotificationWorker] Нет провайдеров с включёнными напоминаниями")
                return Result.success()
            }

            println("🔍 [NotificationWorker] Провайдеров с напоминаниями: ${providersWithReminders.size}")

            // ============================================
            // ПРОВЕРКА КАЖДОГО ПРОВАЙДЕРА
            // ============================================

            for (provider in providersWithReminders) {
                println("🔔 [NotificationWorker] Проверяем провайдера: ${provider.name}")

                // ============================================
                // ПРОВЕРКА ПЕРИОДА (зависит от режима)
                // ============================================

                val isInReminderPeriod = when (globalSettings.reminderPeriodMode) {
                    "AUTO" -> {
                        // ✅ Автоматический режим: грузим период с сайта
                        val startDay = provider.transmissionPeriodStartDay
                        val endDay = provider.transmissionPeriodEndDay

                        if (startDay == null || endDay == null) {
                            println("⚠️ [NotificationWorker] Период не загружен для ${provider.name}")
                            false
                        } else {
                            // Вычисляем день начала напоминаний
                            val reminderStartDay = maxOf(1, startDay - globalSettings.reminderDaysBeforeStart)

                            val inPeriod = currentDay >= reminderStartDay && currentDay <= endDay
                            println("   Период передачи: $startDay-$endDay")
                            println("   Напоминания с: $reminderStartDay (за ${globalSettings.reminderDaysBeforeStart} дней)")
                            println("   Текущий день: $currentDay → ${if (inPeriod) "В ПЕРИОДЕ" else "ВНЕ ПЕРИОДА"}")
                            inPeriod
                        }
                    }

                    "MANUAL" -> {
                        // ✅ Ручной режим: используем customStartDay
                        val customStart = provider.reminderCustomStartDay
                        val endDay = provider.transmissionPeriodEndDay

                        if (customStart == null || endDay == null) {
                            println("⚠️ [NotificationWorker] Кастомный день не настроен для ${provider.name}")
                            false
                        } else {
                            val inPeriod = currentDay >= customStart && currentDay <= endDay
                            println("   Напоминания с: $customStart (ручная настройка)")
                            println("   Период передачи до: $endDay")
                            println("   Текущий день: $currentDay → ${if (inPeriod) "В ПЕРИОДЕ" else "ВНЕ ПЕРИОДА"}")
                            inPeriod
                        }
                    }

                    else -> {
                        println("❌ [NotificationWorker] Неизвестный режим: ${globalSettings.reminderPeriodMode}")
                        false
                    }
                }

                if (!isInReminderPeriod) {
                    println("⏭️ [NotificationWorker] ${provider.name}: вне периода напоминаний")
                    continue
                }

                // ============================================
                // ПРОВЕРКА НЕПЕРЕДАННЫХ СЧЁТЧИКОВ
                // ============================================

                try {
                    val accounts = accountRepository.getAllAccounts().first()
                        .filter { it.providerId == provider.id }

                    if (accounts.isEmpty()) {
                        println("⏭️ [NotificationWorker] Нет аккаунтов для провайдера ${provider.name}")
                        continue
                    }

                    var unsubmittedCount = 0

                    for (account in accounts) {
                        val meters = meterDao.getAllByAccountId(account.id).first()

                        for (meter in meters) {
                            val lastSubmissionMonth = meter.lastSubmissionDate?.let {
                                parseMonthFromDate(it)
                            }

                            if (lastSubmissionMonth != currentMonth) {
                                unsubmittedCount++
                            }
                        }
                    }

                    // ============================================
                    // ПОКАЗ НАПОМИНАНИЯ (если есть непереданные)
                    // ============================================

                    if (unsubmittedCount > 0) {
                        // ✅ Проверка ТОЛЬКО уведомлений провайдера (уровень 2 и 3)
                        // globalNotificationsEnabled УЖЕ проверен в начале метода!

                        if (!globalSettings.providerNotificationsEnabled) {
                            println("⏭️ [NotificationWorker] Уведомления провайдеров отключены глобально")
                            continue
                        }

                        if (!provider.notificationsEnabled) {
                            println("⏭️ [NotificationWorker] Уведомления отключены для ${provider.name}")
                            continue
                        }

                        // ✅ ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ → ПОКАЗЫВАЕМ НАПОМИНАНИЕ
                        notificationHelper.showReadingReminderNotification(
                            providerName = provider.name,
                            meterCount = unsubmittedCount,
                            endDay = provider.transmissionPeriodEndDay ?: 0
                        )
                        println("✅ [NotificationWorker] Напоминание показано: ${provider.name}, счётчиков: $unsubmittedCount")
                    } else {
                        println("✅ [NotificationWorker] Все показания переданы для ${provider.name}")
                    }

                } catch (e: Exception) {
                    println("❌ [NotificationWorker] Ошибка для провайдера ${provider.name}: ${e.message}")
                    e.printStackTrace()
                }
            }

            println("✅ [NotificationWorker] Проверка завершена")
            Result.success()

        } catch (e: Exception) {
            println("❌ [NotificationWorker] Общая ошибка: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }


    /**
     * Парсинг месяца из даты формата "dd.MM.yyyy"
     *
     * @param timestamp Дата в формате timestamp
     * @return Месяц в формате "yyyy-MM" или null при ошибке
     */
    private fun parseMonthFromDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault())
            .format(java.util.Date(timestamp))
    }

    companion object {
        private const val TAG = "NotificationWorker"
        private const val WORK_NAME = "meter_reading_reminder_work"

        /**
         * Запустить ежедневные напоминания
         *
         * @param context Контекст приложения
         * @param hour Час напоминания (0-23)
         * @param minute Минута напоминания (0-59)
         */
        fun schedule(context: Context, hour: Int, minute: Int) {
            // ✅ Вычисляем задержку до следующего запуска с Calendar
            val now = Calendar.getInstance()
            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Если время уже прошло сегодня - запускаем завтра
            if (targetTime.before(now) || targetTime.equals(now)) {
                targetTime.add(Calendar.DAY_OF_MONTH, 1)
            }

            val initialDelay = targetTime.timeInMillis - now.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Требуется интернет для проверки
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MeterReadingNotificationWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )

            println("✅ [NotificationWorker] Запланировано напоминание ежедневно в $hour:${minute.toString().padStart(2, '0')}")
        }

        /**
         * Отменить напоминания
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            println("🛑 [NotificationWorker] Напоминания отменены")
        }
    }
}
