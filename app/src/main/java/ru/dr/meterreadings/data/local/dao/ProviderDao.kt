package ru.dr.meterreadings.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dr.meterreadings.data.local.entities.ProviderEntity

/**
 * DAO (Data Access Object) для работы с таблицей providers
 */
@Dao
interface ProviderDao {

    @Query("SELECT * FROM providers ORDER BY type ASC, name ASC")
    fun getAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    fun getById(id: String): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers WHERE type = :type ORDER BY name ASC")
    fun getAllByType(type: String): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providers: List<ProviderEntity>)

    @Update
    suspend fun update(provider: ProviderEntity)

    @Delete
    suspend fun delete(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM providers")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun getCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM providers WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM providers WHERE name = :name)")
    suspend fun existsByName(name: String): Boolean
}
