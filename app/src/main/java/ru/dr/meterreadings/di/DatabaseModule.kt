package ru.dr.meterreadings.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.dr.meterreadings.data.local.AppDatabase
import ru.dr.meterreadings.data.local.dao.AccountDao
import ru.dr.meterreadings.data.local.dao.AppSettingsDao
import ru.dr.meterreadings.data.local.dao.MeterDao
import ru.dr.meterreadings.data.local.dao.ProfileDao
import ru.dr.meterreadings.data.local.dao.ProviderDao
import javax.inject.Singleton

/**
 * Hilt Module для предоставления Database и DAO
 *
 * Использует AppDatabase.getInstance() для гарантии, что
 * создаётся ОДИН экземпляр БД (независимо от способа доступа)
 */
@Module
@InstallIn(SingletonComponent::class)
object бDatabaseModule {

    /**
     * Предоставляет AppDatabase (Singleton)
     *
     * Использует AppDatabase.getInstance() вместо прямого
     * Room.databaseBuilder() чтобы гарантировать единственный
     * экземпляр БД во всём приложении
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)  // ← ИЗМЕНИЛИ: используем наш Singleton
    }

    /**
     * Предоставляет ProfileDao
     */
    @Provides
    @Singleton
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    /**
     * Предоставляет AccountDao
     */
    @Provides
    @Singleton
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    /**
     * Предоставляет ProviderDao
     */
    @Provides
    @Singleton
    fun provideProviderDao(database: AppDatabase): ProviderDao {
        return database.providerDao()
    }

    /**
     * Предоставляет MeterDao
     */
    @Provides
    @Singleton
    fun provideMeterDao(database: AppDatabase): MeterDao {
        return database.meterDao()
    }

    /**
     * Предоставить AppSettingsDao из базы данных.
     *
     * Hilt автоматически создаст и внедрит этот DAO
     * в AppSettingsRepository через конструктор.
     */
    @Provides
    @Singleton
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }

}
