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
import ru.dr.meterreadings.data.repository.ProfileRepository
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
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val meterDao: MeterDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            println("🔔 [NotificationWorker] Проверка напоминаний о передаче показаний")

            // ✅ ИСПОЛЬЗУЕМ Calendar вместо LocalDate
            val calendar = Calendar.getInstance()
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)

            println("📅 [NotificationWorker] Текущая дата: $today")

            // Получаем провайдеров, у которых сейчас период передачи
            val providersInPeriod = profileRepository.getProvidersInTransmissionPeriod()
            println("🔍 [NotificationWorker] Провайдеров в периоде: ${providersInPeriod.size}")

            for (provider in providersInPeriod) {
                // Пропускаем, если напоминания отключены
                if (!provider.reminderEnabled) {
                    println("⏭️ [NotificationWorker] Провайдер ${provider.name}: напоминания отключены")
                    continue
                }

                println("🔔 [NotificationWorker] Проверяем провайдера: ${provider.name}")

                try {
                    // Находим все аккаунты этого провайдера
                    val accounts = accountRepository.getAllAccounts().first()
                        .filter { it.providerId == provider.id }

                    if (accounts.isEmpty()) {
                        println("⏭️ [NotificationWorker] Нет аккаунтов для провайдера ${provider.name}")
                        continue
                    }

                    println("📋 [NotificationWorker] Аккаунтов: ${accounts.size}")

                    // Проверяем счётчики по всем аккаунтам
                    var unsubmittedCount = 0

                    for (account in accounts) {
                        val meters = meterDao.getAllByAccountId(account.id).first()
                        println("  Аккаунт ${account.accountNumber}: счётчиков ${meters.size}")

                        for (meter in meters) {
                            // Проверяем, передавался ли счётчик в этом месяце
                            val lastSubmissionMonth = meter.lastSubmissionDate?.let {
                                parseMonthFromDate(it)
                            }

                            if (lastSubmissionMonth != currentMonth) {
                                unsubmittedCount++
                                println("  ⚠️ Счётчик ${meter.type} №${meter.serialNumber}: не передан в $currentMonth")
                            }
                        }
                    }

                    // Если есть непереданные - показываем уведомление
                    if (unsubmittedCount > 0) {
                        notificationHelper.showReadingReminderNotification(
                            providerName = provider.name,
                            meterCount = unsubmittedCount,
                            endDay = provider.transmissionPeriodEndDay ?: 0
                        )
                        println("✅ [NotificationWorker] Уведомление показано: ${provider.name}, счётчиков: $unsubmittedCount")
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
     * @param date Дата в формате "15.01.2026"
     * @return Месяц в формате "yyyy-MM" или null при ошибке
     */
    private fun parseMonthFromDate(date: String): String? {
        return try {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            parsedDate?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            println("⚠️ [NotificationWorker] Ошибка парсинга месяца из '$date': ${e.message}")
            null
        }
    }

    companion object {
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
