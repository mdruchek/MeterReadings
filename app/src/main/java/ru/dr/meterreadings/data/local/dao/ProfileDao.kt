// app/src/main/java/ru/dr/meterreadings/data/local/dao/ProfileDao.kt

package ru.dr.meterreadings.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dr.meterreadings.data.local.entities.ProfileEntity

@Dao
interface ProfileDao {

    /**
     * Получить все профили (Flow для автообновления UI)
     */
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAll(): Flow<List<ProfileEntity>>

    /**
     * Получить профиль по ID (Flow)
     */
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    fun getById(profileId: String): Flow<ProfileEntity?>

    /**
     * ✅ НОВОЕ: Получить профили с количеством аккаунтов
     *
     * Возвращает профили и количество связанных аккаунтов
     * для отображения в списке профилей
     */
    @Query("""
        SELECT 
            p.*,
            COUNT(a.id) as accountCount
        FROM profiles p
        LEFT JOIN accounts a ON p.id = a.profileId
        GROUP BY p.id
        ORDER BY p.name ASC
    """)
    fun getProfilesWithAccountCount(): Flow<List<ProfileWithAccountCount>>

    /**
     * Вставить новый профиль
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    /**
     * Обновить профиль
     */
    @Update
    suspend fun update(profile: ProfileEntity)

    /**
     * Удалить профиль по ID
     * Связанные аккаунты удалятся автоматически (CASCADE)
     */
    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteById(profileId: String)

    /**
     * Удалить все профили
     */
    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}

/**
 * ✅ НОВОЕ: Data class для результата запроса с JOIN
 *
 * Room автоматически заполнит эти поля из SELECT запроса
 */
data class ProfileWithAccountCount(
    @Embedded val profile: ProfileEntity,
    val accountCount: Int
)
