package ru.dr.meterreadings.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.dr.meterreadings.data.local.AppDatabase
import ru.dr.meterreadings.data.local.dao.ProfileDao
import javax.inject.Singleton

/**
 * Hilt Module для предоставления Database и DAO
 *
 * Этот модуль говорит Hilt:
 * - Как создать AppDatabase (один раз на всё приложение)
 * - Как получить ProfileDao из AppDatabase
 */
@Module  // Это Hilt Module (содержит инструкции по созданию зависимостей)
@InstallIn(SingletonComponent::class)  // Живёт всё время жизни приложения
object DatabaseModule {

    /**
     * Предоставляет AppDatabase (Singleton)
     *
     * @Provides - Hilt будет вызывать эту функцию для создания AppDatabase
     * @Singleton - создать один раз, переиспользовать везде
     * @ApplicationContext - Context всего приложения (не Activity!)
     *
     * Room.databaseBuilder создаёт БД:
     * - context - где создать файл БД
     * - AppDatabase::class.java - класс БД
     * - "meter_readings.db" - имя файла БД
     * - .build() - построить и вернуть
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "meter_readings.db"  // Имя файла базы данных
        )
            .fallbackToDestructiveMigration()  // При ошибке миграции - удалить БД
            // Раскомментируй ↑ только для разработки!
            // В продакшене используй Migration
            .build()
    }

    /**
     * Предоставляет ProfileDao
     *
     * Параметр database: AppDatabase - Hilt автоматически передаст
     * (он знает как создать AppDatabase из функции выше)
     *
     * Возвращает ProfileDao из AppDatabase
     */
    @Provides
    @Singleton
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    // Позже добавим провайдеры для других DAO:
    // @Provides
    // @Singleton
    // fun provideAccountDao(database: AppDatabase): AccountDao {
    //     return database.accountDao()
    // }
}
