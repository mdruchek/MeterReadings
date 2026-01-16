// app/src/main/java/ru/dr/meterreadings/data/repository/ProviderRepository.kt

package ru.dr.meterreadings.data.repository

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.ProviderDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с провайдерами услуг
 */
@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val httpClient: HttpClient
) {

    // ========================================
    // ЧТЕНИЕ (READ)
    // ========================================

    /**
     * Получить все провайдеры как Flow (с автообновлением)
     */
    fun getAllProviders(): Flow<List<ProviderDomainModel>> {
        return providerDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Получить провайдера по ID
     *
     * @param id ID провайдера (String, например ProviderIds.KVC)
     */
    fun getProviderById(id: String): Flow<ProviderDomainModel?> {  // ✅ ИСПРАВЛЕНО: String → Long
        return providerDao.getById(id).map { it?.toDomain() }
    }

    /**
     * Получить провайдеров по типу услуги
     *
     * @param type Enum Type (WaterSupply, GasSupply, etc)
     */
    fun getProvidersByType(type: Type): Flow<List<ProviderDomainModel>> {
        // ✅ ИЗМЕНЕНИЕ: Конвертируем enum в строку для запроса к БД
        return providerDao.getAllByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Поиск провайдеров по названию
     */
    fun searchProviders(query: String): Flow<List<ProviderDomainModel>> {
        return providerDao.searchByName(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // ========================================
    // ЗАПИСЬ (WRITE)
    // ========================================

    /**
     * Добавить нового провайдера
     */
    suspend fun addProvider(provider: ProviderDomainModel) {
        val exists = providerDao.exists(provider.id)
        if (exists) {
            throw IllegalArgumentException("Provider with id '${provider.id}' already exists")
        }

        val entity = provider.toEntity()
        providerDao.insert(entity)
        println("✅ [ProviderRepository] Провайдер добавлен: ${provider.name}")  // ✅ ДОБАВЛЕНО
    }

    /**
     * Обновить провайдера
     */
    suspend fun updateProvider(provider: ProviderDomainModel) {
        val existingEntity = providerDao.getById(provider.id).first()
            ?: throw IllegalArgumentException("Provider not found")

        val updatedEntity = provider.toEntity(
            createdAt = existingEntity.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        providerDao.update(updatedEntity)
        println("✅ [ProviderRepository] Провайдер обновлён: ${provider.name}")  // ✅ ДОБАВЛЕНО
    }

    /**
     * Удалить провайдера
     *
     * @param id ID провайдера (Long, например ProviderIds.KVC)
     */
    suspend fun deleteProvider(id: Long) {  // ✅ ИСПРАВЛЕНО: String → Long
        providerDao.deleteById(id)
        println("✅ [ProviderRepository] Провайдер удалён: $id")  // ✅ ДОБАВЛЕНО
    }

    // ========================================
    // СПЕЦИАЛЬНЫЕ ОПЕРАЦИИ
    // ========================================

    /**
     * Получить количество провайдеров
     */
    suspend fun getProviderCount(): Int {
        return providerDao.getCount()
    }

    /**
     * Обновить период передачи показаний для провайдера.
     * Обновляет только если период ещё не загружен для текущего месяца.
     *
     * @param providerId ID провайдера (Long)
     * @param periodStartDay День начала периода (например 15)
     * @param periodEndDay День окончания периода (например 22)
     */
    suspend fun updateProviderTransmissionPeriod(
        providerId: Long,
        periodStartDay: Int,
        periodEndDay: Int
    ) {
        try {
            val provider = providerDao.getById(providerId.toString()).first()

            if (provider != null) {
                val currentMonth = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"))

                val updated = provider.copy(
                    transmissionPeriodStartDay = periodStartDay,
                    transmissionPeriodEndDay = periodEndDay,
                    lastPeriodUpdate = System.currentTimeMillis(),
                    periodLoadedForMonth = currentMonth,
                    updatedAt = System.currentTimeMillis()
                )

                providerDao.update(updated)

                println("✅ [ProviderRepository] Период обновлён для провайдера $providerId")
                println("   Дни: $periodStartDay - $periodEndDay")
                println("   Месяц: $currentMonth")
            }
        } catch (e: Exception) {
            println("❌ [ProviderRepository] Ошибка обновления периода: ${e.message}")
            e.printStackTrace()
        }
    }
}
