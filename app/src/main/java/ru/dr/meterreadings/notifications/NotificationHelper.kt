// app/src/main/java/ru/dr/meterreadings/notifications/NotificationHelper.kt

package ru.dr.meterreadings.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.dr.meterreadings.R
import ru.dr.meterreadings.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хелпер для управления уведомлениями приложения
 *
 * Типы уведомлений:
 * 1. Напоминание о передаче показаний
 * 2. Уведомление об обновлении периода передачи
 * 3. Ошибки синхронизации (опционально)
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        // Каналы уведомлений
        private const val CHANNEL_REMINDERS = "meter_reading_reminders"
        private const val CHANNEL_UPDATES = "period_updates"
        private const val CHANNEL_ERRORS = "sync_errors"

        // ID уведомлений
        private const val NOTIFICATION_REMINDER = 100
        private const val NOTIFICATION_PERIOD_UPDATE = 101
        private const val NOTIFICATION_ERROR = 102
    }

    init {
        createNotificationChannels()
    }

    /**
     * Создать каналы уведомлений (Android 8.0+)
     *
     * Каналы позволяют пользователю настроить приоритет,
     * звук, вибрацию для разных типов уведомлений
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                // Канал для напоминаний о передаче показаний
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Напоминания о передаче показаний",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Напоминания о необходимости передать показания счётчиков"
                    enableVibration(true)
                    enableLights(true)
                },

                // Канал для обновлений периода
                NotificationChannel(
                    CHANNEL_UPDATES,
                    "Обновления периода передачи",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Уведомления об автоматическом обновлении периода передачи"
                    enableVibration(false)
                },

                // Канал для ошибок
                NotificationChannel(
                    CHANNEL_ERRORS,
                    "Ошибки синхронизации",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Уведомления об ошибках при работе с провайдерами"
                    enableVibration(false)
                }
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { manager.createNotificationChannel(it) }

            println("✅ [NotificationHelper] Каналы уведомлений созданы")
        }
    }

    /**
     * Показать напоминание о передаче показаний
     *
     * Вызывается из MeterReadingNotificationWorker
     *
     * @param providerName Название провайдера (например, "КВЦ")
     * @param meterCount Количество непереданных счётчиков
     * @param endDay День окончания периода передачи
     */
    fun showReadingReminderNotification(
        providerName: String,
        meterCount: Int,
        endDay: Int
    ) {
        // Проверяем разрешение на уведомления (Android 13+)
        if (!hasNotificationPermission()) {
            println("⚠️ [NotificationHelper] Нет разрешения на уведомления")
            return
        }

        // Интент для открытия приложения при нажатии
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Можно добавить deeplink на экран провайдера
            // putExtra("provider_id", providerId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Формируем текст
        val title = "Передайте показания счётчиков"
        val text = buildString {
            append("$providerName: ")
            append(getMeterCountText(meterCount))
            append(". Период до $endDay числа.")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)  // ✅ Нужно создать иконку
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)  // Закрывается при нажатии
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_REMINDER, notification)

        println("✅ [NotificationHelper] Напоминание показано: $providerName, счётчиков: $meterCount")
    }

    /**
     * Показать уведомление об обновлении периода передачи
     *
     * Вызывается из PeriodUpdateWorker после успешного обновления
     *
     * @param providerName Название провайдера
     * @param startDay День начала периода
     * @param endDay День окончания периода
     */
    fun showPeriodUpdatedNotification(
        providerName: String,
        startDay: Int,
        endDay: Int
    ) {
        if (!hasNotificationPermission()) {
            println("⚠️ [NotificationHelper] Нет разрешения на уведомления")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Период передачи обновлён"
        val text = "$providerName: с $startDay по $endDay число"

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_PERIOD_UPDATE, notification)

        println("✅ [NotificationHelper] Уведомление об обновлении показано: $providerName ($startDay-$endDay)")
    }

    /**
     * Показать уведомление об ошибке синхронизации
     *
     * Опциональное уведомление для отладки
     *
     * @param providerName Название провайдера
     * @param errorMessage Текст ошибки
     */
    fun showErrorNotification(
        providerName: String,
        errorMessage: String
    ) {
        if (!hasNotificationPermission()) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Ошибка синхронизации"
        val text = "$providerName: $errorMessage"

        val notification = NotificationCompat.Builder(context, CHANNEL_ERRORS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ERROR, notification)

        println("⚠️ [NotificationHelper] Уведомление об ошибке показано: $providerName")
    }

    /**
     * Отменить все уведомления
     *
     * Используется при выходе из приложения или по запросу пользователя
     */
    fun cancelAll() {
        notificationManager.cancelAll()
        println("🛑 [NotificationHelper] Все уведомления отменены")
    }

    /**
     * Отменить конкретное уведомление
     *
     * @param notificationId ID уведомления
     */
    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Проверить, есть ли разрешение на уведомления
     *
     * Android 13+ требует явного разрешения POST_NOTIFICATIONS
     *
     * @return true, если разрешение есть или не требуется
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // До Android 13 разрешение не требуется
            true
        }
    }

    /**
     * Проверить, включены ли уведомления для приложения
     *
     * @return true, если уведомления включены
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Форматировать количество счётчиков в правильную форму
     *
     * Примеры:
     * - 1 → "не передан 1 счётчик"
     * - 2 → "не передано 2 счётчика"
     * - 5 → "не передано 5 счётчиков"
     *
     * @param count Количество счётчиков
     * @return Форматированная строка
     */
    private fun getMeterCountText(count: Int): String {
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "счётчик"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "счётчика"
            else -> "счётчиков"
        }

        val verb = if (count == 1) "не передан" else "не передано"

        return "$verb $count $form"
    }
}
