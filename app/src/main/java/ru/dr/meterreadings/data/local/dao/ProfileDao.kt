package ru.dr.meterreadings.data.local.dao

import androidx.room.*
import ru.dr.meterreadings.data.local.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) для работы с таблицей profiles
 *
 * Room автоматически создаст реализацию всех методов.
 * Все операции асинхронные (suspend) или реактивные (Flow).
 */
@Dao
interface ProfileDao {

    // ========================================
    // ЧТЕНИЕ (SELECT)
    // ========================================

    /**
     * Получить все профили
     *
     * Flow - реактивный поток, автоматически обновляется при изменении БД
     * UI будет автоматически перерисовываться при изменениях!
     */
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAll(): Flow<List<ProfileEntity>>

    /**
     * Получить профиль по ID как Flow (с автообновлением)
     */
    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getById(id: String): Flow<ProfileEntity?>

    // ========================================
    // ДОБАВЛЕНИЕ/ОБНОВЛЕНИЕ (INSERT/UPDATE)
    // ========================================

    /**
     * Вставить новый профиль
     *
     * OnConflictStrategy.REPLACE:
     * - Если профиль с таким ID существует → заменить
     * - Если не существует → создать новый
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    /**
     * Вставить несколько профилей
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    /**
     * Обновить существующий профиль
     *
     * Room найдёт запись по @PrimaryKey (id) и обновит все поля
     */
    @Update
    suspend fun update(profile: ProfileEntity)

    // ========================================
    // УДАЛЕНИЕ (DELETE)
    // ========================================

    /**
     * Удалить профиль (передать объект)
     */
    @Delete
    suspend fun delete(profile: ProfileEntity)

    /**
     * Удалить профиль по ID
     *
     * @Query с DELETE - можно удалить по любому условию
     */
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Удалить все профили (осторожно!)
     */
    @Query("DELETE FROM profiles")
    suspend fun deleteAll()

    // ========================================
    // СПЕЦИАЛЬНЫЕ ЗАПРОСЫ
    // ========================================

    /**
     * Получить количество профилей
     */
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int

    /**
     * Проверить существование профиля с таким именем
     *
     * EXISTS(...) возвращает 1 если найдено, 0 если нет
     * Room автоматически конвертирует в Boolean
     */
    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE name = :name)")
    suspend fun existsByName(name: String): Boolean
}
