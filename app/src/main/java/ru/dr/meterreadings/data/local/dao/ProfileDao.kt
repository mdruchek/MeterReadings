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
    fun getAllFlow(): Flow<List<ProfileEntity>>

    /**
     * Получить все профили (одноразово)
     *
     * suspend - корутина, выполняется асинхронно
     */
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    suspend fun getAll(): List<ProfileEntity>

    /**
     * Получить профиль по ID
     *
     * :id - параметр запроса (подставится значение из параметра функции)
     */
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    /**
     * Получить профиль по умолчанию
     *
     * LIMIT 1 - вернёт только первый результат
     */
    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ProfileEntity?

    /**
     * Получить профиль по ID как Flow (с автообновлением)
     */
    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getByIdFlow(id: String): Flow<ProfileEntity?>

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
     * Сбросить флаг isDefault у всех профилей
     *
     * Нужно перед установкой нового профиля по умолчанию
     */
    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultFlag()

    /**
     * Установить профиль по умолчанию
     *
     * @Transaction - все операции выполнятся атомарно (вместе или никак)
     */
    @Transaction
    suspend fun setDefault(profileId: String) {
        clearDefaultFlag()  // Сначала сбросить у всех
        // Потом установить у нужного
        val profile = getById(profileId)
        if (profile != null) {
            update(profile.copy(isDefault = true))
        }
    }

    /**
     * Получить количество профилей
     */
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int
}
