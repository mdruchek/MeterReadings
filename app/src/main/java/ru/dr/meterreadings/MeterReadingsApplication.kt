package ru.dr.meterreadings

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.ProviderRepository
import javax.inject.Inject

/**
 * Главный класс приложения
 *
 * @HiltAndroidApp - активирует Dependency Injection
 */
@HiltAndroidApp
class MeterReadingsApplication : Application() {
    /**
     * Hilt автоматически внедрит ProviderRepository
     *
     * @Inject - говорит Hilt что нужно создать и передать этот объект
     */
    @Inject
    lateinit var providerRepository: ProviderRepository

    override fun onCreate() {
        super.onCreate()

        println("🚀 [App] Приложение запущено")

        // Инициализируем БД провайдеров при первом запуске
        initializeDatabase()
    }

    /**
     * Заполняем БД провайдеров моковыми данными
     * если таблица пустая
     *
     * Выполняется в фоновом потоке (Dispatchers.IO)
     */
    private fun initializeDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("💾 [App] Проверяем БД провайдеров...")

                // ProviderRepository.initializeWithMockData() сам проверит
                // пустая ли таблица и заполнит если нужно
                providerRepository.initializeWithMockData()

                println("✅ [App] Инициализация БД завершена")
            } catch (e: Exception) {
                println("❌ [App] Ошибка инициализации БД: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
