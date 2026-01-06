package ru.dr.meterreadings.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dr.meterreadings.data.local.dao.AccountDao
import ru.dr.meterreadings.data.local.dao.ProfileDao
import ru.dr.meterreadings.data.local.entities.AccountEntity
import ru.dr.meterreadings.data.local.entities.ProfileEntity

/**
 * Главная база данных приложения
 *
 * @Database - указывает Room что это БД
 * - entities - список всех таблиц
 * - version - версия схемы БД (при изменении структуры нужно увеличить)
 * - exportSchema - сохранять ли схему в JSON (для миграций)
 * Room создаст SQLite базу данных с указанными таблицами.
 * Этот класс - Singleton (один экземпляр на всё приложение).
 */
@Database(
    entities = [
        ProfileEntity::class,  // Список всех таблиц (Entity)
        AccountEntity::class,
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

    /**
     * Получить DAO для работы с аккаунтами
     * Room автоматически создаст реализацию
     */
    abstract fun accountDao(): AccountDao

    companion object {
        /**
         * Singleton instance БД
         * @Volatile - изменения видны всем потокам
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Имя файла БД на устройстве
         */
        private const val DATABASE_NAME = "meter_readings.db"

        /**
         * Получить экземпляр БД (Singleton pattern)
         *
         * synchronized - гарантирует что только один поток
         * создаст БД (thread-safe)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // fallbackToDestructiveMigration - при изменении схемы
                    // удалить старую БД и создать новую (только для разработки!)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
