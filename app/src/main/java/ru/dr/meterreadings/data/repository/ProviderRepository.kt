package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.ProviderDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с провайдерами услуг
 */
@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao
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
     */
    fun getProviderById(id: String): Flow<ProviderDomainModel?> {
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
    }

    /**
     * Удалить провайдера
     */
    suspend fun deleteProvider(id: String) {
        providerDao.deleteById(id)
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
     * Инициализировать БД моковыми провайдерами
     *
     * ✅ ИЗМЕНЕНИЕ: Используем новый enum Type
     */
    suspend fun initializeWithMockData() {
        val count = providerDao.getCount()
        if (count > 0) {
            println("⚠️ [ProviderRepository] БД уже содержит провайдеров: $count")
            return
        }

        println("✨ [ProviderRepository] Инициализация БД моковыми провайдерами")

        val mockProviders = listOf(
            // ✅ ИЗМЕНЕНИЕ: Используем Type.WaterSupply вместо строки
            ProviderDomainModel(
                id = "mosvodokanal",
                name = "Мосводоканал",
                type = Type.WaterSupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://mosvodokanal.me/",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "mosenergosby",
                name = "Мосэнергосбыт",
                type = Type.ElectricitySupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://mosenergosby.me/",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "mosoblgaz",
                name = "Мособлгаз",
                type = Type.GasSupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://mosoblgaz.me/",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "energosbyt",
                name = "Энергосбыт",
                type = Type.ElectricitySupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://energosbyt.ru",
                authType = AuthType.API_KEY
            ),
            ProviderDomainModel(
                id = "gazprom",
                name = "Газпром Межрегионгаз",
                type = Type.GasSupply,  // ✅ Enum!
                logoUrl = null,
                baseUrl = "https://gazprom.ru",
                authType = AuthType.AUTH_REQUIRED
            )
        )

        val entities = mockProviders.map { it.toEntity() }
        providerDao.insertAll(entities)

        println("✅ [ProviderRepository] Добавлено ${mockProviders.size} провайдеров")
    }
}
