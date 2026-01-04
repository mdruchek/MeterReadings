package ru.dr.meterreadings.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.dr.meterreadings.data.local.dao.ProfileDao
import ru.dr.meterreadings.data.local.entities.ProfileEntity

/**
 * Главный класс базы данных приложения
 *
 * Room создаст SQLite базу данных с указанными таблицами.
 * Этот класс - Singleton (один экземпляр на всё приложение).
 */
@Database(
    entities = [
        ProfileEntity::class  // Список всех таблиц (Entity)
        // Позже добавим: AccountEntity, MeterEntity, ReadingEntity
    ],
    version = 1,  // Версия БД (при изменении схемы увеличивать)
    exportSchema = false  // Не экспортировать схему БД (для простоты)
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Получить DAO для работы с профилями
     *
     * Room автоматически создаст реализацию ProfileDao
     */
    abstract fun profileDao(): ProfileDao

    // Позже добавим:
    // abstract fun accountDao(): AccountDao
    // abstract fun meterDao(): MeterDao
    // abstract fun readingDao(): ReadingDao
}
