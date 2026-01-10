package ru.dr.meterreadings.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.remote.dto.KvcRegionDto
import ru.dr.meterreadings.data.local.dao.ProviderDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.AuthType
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.domain.Type
import ru.dr.meterreadings.data.remote.dto.CounterForInsertDto
import ru.dr.meterreadings.data.remote.dto.GetAbonentInfoRequest
import ru.dr.meterreadings.data.remote.dto.GetCntListRequest
import ru.dr.meterreadings.data.remote.dto.GetCtrDaysRequest
import ru.dr.meterreadings.data.remote.dto.InsertCtrRequest
import ru.dr.meterreadings.data.remote.dto.KvcCounterDto
import ru.dr.meterreadings.data.remote.dto.KvcLocationDto
import ru.dr.meterreadings.data.remote.dto.KvcAbonentInfoDto
import ru.dr.meterreadings.data.remote.dto.KvcTransitDaysDto
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
}
