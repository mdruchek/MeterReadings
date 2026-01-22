// app/src/main/java/ru/dr/meterreadings/workers/PeriodUpdateWorker.kt

package ru.dr.meterreadings.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.domain.connector.GetTransmissionPeriod
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.notifications.NotificationHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Worker для автоматического обновления периода передачи показаний
 *
 * Логика работы:
 * 1. Проверяет всех провайдеров с включённым autoUpdateEnabled
 * 2. Для каждого провайдера:
 *    - Проверяет, наступил ли день начала обновления (updateStartDay)
 *    - Проверяет, не загружен ли уже период для текущего месяца
 *    - Загружает первый аккаунт этого провайдера
 *    - Загружает период через коннектор провайдера
 *    - Сохраняет в БД
 * 3. Показывает уведомление об обновлении (если включено)
 *
 * Запускается периодически с интервалом из настроек провайдера
 */
@HiltWorker
class PeriodUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val providerRepository: ProviderRepository,
    private val accountRepository: AccountRepository,
    private val connectorFactory: ProviderConnectorFactory,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            println("🔄 [PeriodUpdateWorker] Начало проверки обновлений периода")

            val today = LocalDate.now()
            val todayDay = today.dayOfMonth
            val currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))

            println("📅 [PeriodUpdateWorker] Текущая дата: $today (день: $todayDay, месяц: $currentMonth)")

            // Получаем всех провайдеров
            val providers = providerRepository.getAllProviders().first()
            println("🔍 [PeriodUpdateWorker] Провайдеров в БД: ${providers.size}")

            var updatedCount = 0

            for (provider in providers) {
                // Пропускаем, если автообновление отключено
                if (!provider.autoUpdateEnabled) {
                    println("⏭️ [PeriodUpdateWorker] Провайдер ${provider.name}: автообновление отключено")
                    continue
                }

                // Пропускаем, если ещё не наступил день начала обновления
                if (todayDay < provider.updateStartDay) {
                    println("⏭️ [PeriodUpdateWorker] Провайдер ${provider.name}: день обновления ещё не наступил (старт: ${provider.updateStartDay})")
                    continue
                }

                // Пропускаем, если период уже загружен для текущего месяца
                if (provider.periodLoadedForMonth == currentMonth) {
                    println("⏭️ [PeriodUpdateWorker] Провайдер ${provider.name}: период уже загружен для месяца $currentMonth")
                    continue
                }

                println("🔄 [PeriodUpdateWorker] Провайдер ${provider.name}: начинаем обновление...")

                try {
                    // Загружаем любой аккаунт этого провайдера
                    val accounts = accountRepository.getAllAccounts().first()
                    val account = accounts.firstOrNull { it.providerId == provider.id }

                    if (account == null) {
                        println("⚠️ [PeriodUpdateWorker] Провайдер ${provider.name}: нет аккаунтов")
                        continue
                    }

                    println("📋 [PeriodUpdateWorker] Используем аккаунт: ${account.accountNumber}")

                    // Получаем коннектор для провайдера
                    val connector = connectorFactory.getConnector(provider.id)

                    // Проверяем, поддерживает ли провайдер получение периода
                    if (connector is GetTransmissionPeriod) {
                        val periodResult = connector.getTransmissionPeriod(
                            accountNumber = account.accountNumber,
                            regionId = account.regionId
                        )

                        if (periodResult.isSuccess) {
                            val period = periodResult.getOrThrow()

                            // Сохраняем в БД
                            providerRepository.updateProviderTransmissionPeriod(
                                providerId = provider.id,
                                periodStartDay = period.startDay,
                                periodEndDay = period.endDay
                            )

                            updatedCount++
                            println("✅ [PeriodUpdateWorker] Период обновлён для ${provider.name}: ${period.startDay}-${period.endDay}")

                            // Показываем уведомление
//                            if (provider.updateNotificationsEnabled) {
//                                val newProvider = profileRepository.getProviderById(provider.id).first()
//                                if (newProvider != null) {
//                                    notificationHelper.showPeriodUpdatedNotification(
//                                        providerName = provider.name,
//                                        startDay = newProvider.transmissionPeriodStartDay ?: 0,
//                                        endDay = newProvider.transmissionPeriodEndDay ?: 0
//                                    )
//                                }
//                            }
                        } else {
                            println("❌ [PeriodUpdateWorker] Ошибка: ${periodResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        println("⏭️ [PeriodUpdateWorker] Провайдер ${provider.name} не поддерживает автообновление периода")
                    }

                } catch (e: Exception) {
                    println("❌ [PeriodUpdateWorker] Ошибка для провайдера ${provider.name}: ${e.message}")
                    e.printStackTrace()
                }
            }

            println("✅ [PeriodUpdateWorker] Обновлено провайдеров: $updatedCount")

            Result.success()

        } catch (e: Exception) {
            println("❌ [PeriodUpdateWorker] Общая ошибка: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "period_update_work"

        /**
         * Запустить периодическое обновление периода
         *
         * @param context Контекст приложения
         * @param intervalHours Интервал проверки в часах (1, 6, 12, 24)
         */
        fun schedule(context: Context, intervalHours: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Требуется интернет
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PeriodUpdateWorker>(
                repeatInterval = intervalHours.toLong(),
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,  // Заменяем старую задачу
                    workRequest
                )

            println("✅ [PeriodUpdateWorker] Запланировано обновление каждые $intervalHours ч")
        }

        /**
         * Отменить периодическое обновление
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            println("🛑 [PeriodUpdateWorker] Обновление отменено")
        }
    }
}
