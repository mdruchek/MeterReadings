package ru.dr.meterreadings

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.dr.meterreadings.data.database.DatabaseInitializer
import javax.inject.Inject

@HiltAndroidApp
class MeterReadingsApplication : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    override fun onCreate() {
        super.onCreate()

        // Инициализируем провайдеров при первом запуске
        databaseInitializer.initializeProviders()
    }
}
