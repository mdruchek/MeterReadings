// data/util/LogFileManager.kt
package ru.dr.meterreadings.data.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// data/util/LogFileManager.kt
@Singleton
class LogFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logFile: File
        get() = File(context.filesDir, "app_logs.txt")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    // ✅ ФЛАГ ДЛЯ ВКЛЮЧЕНИЯ/ОТКЛЮЧЕНИЯ ЛОГИРОВАНИЯ
    @Volatile
    private var isLoggingEnabled = true

    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
    }

    /**
     * Записать лог в файл
     */
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        if (!isLoggingEnabled) return // ← Проверка

        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = "[$timestamp] [${level.name}] [$tag] $message\n"

            logFile.appendText(logEntry)

            // Ограничиваем размер файла (10 МБ)
            limitFileSize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Записать ошибку с stack trace
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (!isLoggingEnabled) return // ← Проверка

        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = buildString {
                append("[$timestamp] [ERROR] [$tag] $message\n")
                if (throwable != null) {
                    append("Exception: ${throwable.javaClass.simpleName}: ${throwable.message}\n")
                    append(throwable.stackTraceToString())
                    append("\n")
                }
            }

            logFile.appendText(logEntry)
            limitFileSize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Очистить логи старше указанного количества дней
     */
    fun clearOldLogs(retentionDays: Int) {
        if (retentionDays == 0) return // 0 = не удалять автоматически

        try {
            if (!logFile.exists()) return

            val cutoffDate = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
            val cutoffDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(cutoffDate))

            val lines = logFile.readLines()
            val filteredLines = lines.filter { line ->
                // Извлекаем дату из строки вида [2026-01-25 18:06:23.456]
                val dateMatch = Regex("""\[(\d{4}-\d{2}-\d{2})""").find(line)
                if (dateMatch != null) {
                    val logDateStr = dateMatch.groupValues[1]
                    logDateStr >= cutoffDateStr
                } else {
                    true // Оставляем строки без даты
                }
            }

            if (filteredLines.size < lines.size) {
                logFile.writeText(filteredLines.joinToString("\n") + "\n")
                log("LogFileManager", "Удалено ${lines.size - filteredLines.size} старых записей", LogLevel.INFO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Получить содержимое логов
     */
    fun getLogContent(): String {
        return try {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                "Логов нет"
            }
        } catch (e: Exception) {
            "Ошибка чтения логов: ${e.message}"
        }
    }

    /**
     * Получить последние N строк
     */
    fun getLastLines(count: Int = 1000): String {
        return try {
            if (logFile.exists()) {
                val lines = logFile.readLines()
                lines.takeLast(count).joinToString("\n")
            } else {
                "Логов нет"
            }
        } catch (e: Exception) {
            "Ошибка чтения логов: ${e.message}"
        }
    }

    /**
     * Поделиться файлом логов
     */
    fun shareLogFile(activity: Activity) {
        try {
            if (!logFile.exists() || logFile.length() == 0L) {
                Toast.makeText(activity, "Нет логов для отправки", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MeterReadings App Logs")
                putExtra(Intent.EXTRA_TEXT, "Логи приложения MeterReadings")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(Intent.createChooser(intent, "Отправить логи через..."))
        } catch (e: Exception) {
            Toast.makeText(activity, "Ошибка отправки: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /**
     * Очистить все логи
     */
    fun clearLogs() {
        try {
            val wasEnabled = isLoggingEnabled
            isLoggingEnabled = true // Временно включаем для записи сообщения об очистке

            logFile.writeText("")
            log("LogFileManager", "Логи очищены вручную", LogLevel.INFO)

            isLoggingEnabled = wasEnabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Получить размер файла логов
     */
    fun getLogFileSize(): String {
        return try {
            if (logFile.exists()) {
                val sizeInBytes = logFile.length()
                when {
                    sizeInBytes < 1024 -> "$sizeInBytes B"
                    sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
                    else -> "${sizeInBytes / (1024 * 1024)} MB"
                }
            } else {
                "0 B"
            }
        } catch (e: Exception) {
            "Неизвестно"
        }
    }

    /**
     * Получить количество строк в логах
     */
    fun getLogLineCount(): Int {
        return try {
            if (logFile.exists()) {
                logFile.readLines().size
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Ограничить размер файла (оставляем последние 5000 строк)
     */
    private fun limitFileSize() {
        try {
            val maxSize = 10 * 1024 * 1024 // 10 МБ
            if (logFile.length() > maxSize) {
                val lines = logFile.readLines()
                val newContent = lines.takeLast(5000).joinToString("\n")
                logFile.writeText(newContent + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
}
