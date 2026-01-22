// app/src/main/java/ru/dr/meterreadings/data/local/dao/ProviderDao.kt

package ru.dr.meterreadings.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dr.meterreadings.data.local.entities.ProviderEntity

@Dao
interface ProviderDao {

    @Query("SELECT * FROM providers ORDER BY name ASC")
    fun getAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    fun getById(id: Long): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers WHERE type = :type ORDER BY name ASC")
    fun getAllByType(type: String): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ProviderEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM providers WHERE id = :id)")
    suspend fun exists(id: Long): Boolean  // ✅ ИСПРАВЛЕНО: String → Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providers: List<ProviderEntity>)

    @Update
    suspend fun update(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: Long)  // ✅ ИСПРАВЛЕНО: String → Long

    @Query("DELETE FROM providers")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun getCount(): Int
}
