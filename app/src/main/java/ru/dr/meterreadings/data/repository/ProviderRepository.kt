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
import java.util.Calendar
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
    fun getProviderById(id: Long): Flow<ProviderDomainModel?> {  // ✅ ИСПРАВЛЕНО: String → Long
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

    /**
     * Обновить настройки уведомлений для конкретного провайдера.
     *
     * Позволяет обновить один или оба флага уведомлений.
     * Если параметр null — поле не изменяется.
     *
     * @param providerId ID провайдера
     * @param natificationsEnabled Уведомления провайдера (null = не менять)
     */
    suspend fun updateProviderNotifications(
        providerId: Long,
        updateNotificationsEnabled: Boolean?
    ) {
        try {
            // Получаем текущего провайдера из БД
            val provider = providerDao.getById(providerId).first()
                ?: throw IllegalArgumentException("Provider not found: $providerId")

            // Создаём обновлённый объект, меняя только переданные поля
            val updatedProvider = provider.copy(
                notificationsEnabled = updateNotificationsEnabled ?: provider.notificationsEnabled,
                updatedAt = System.currentTimeMillis()
            )

            // Сохраняем в БД
            providerDao.update(updatedProvider)

            println("✅ [ProviderRepository] Уведомления провайдера обновлены: $providerId")
            println("   updateNotifications: ${updatedProvider.notificationsEnabled}")

        } catch (e: Exception) {
            println("❌ [ProviderRepository] Ошибка обновления уведомлений: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Обновить кастомный день напоминания для провайдера (режим MANUAL).
     *
     * @param providerId ID провайдера
     * @param day День месяца (1-31) или null для сброса
     */
    suspend fun updateProviderReminderDay(
        providerId: Long,
        day: Int?
    ) {
        try {
            // Валидация дня месяца
            if (day != null && (day < 1 || day > 31)) {
                throw IllegalArgumentException("День должен быть от 1 до 31, получено: $day")
            }

            // Получаем текущего провайдера из БД
            val provider = providerDao.getById(providerId).first()
                ?: throw IllegalArgumentException("Provider not found: $providerId")

            // Создаём обновлённый объект
            val updatedProvider = provider.copy(
                reminderCustomStartDay = day,
                updatedAt = System.currentTimeMillis()
            )

            // Сохраняем в БД
            providerDao.update(updatedProvider)
            println("✅ [ProviderRepository] День напоминания обновлён для провайдера $providerId: $day")
        } catch (e: Exception) {
            println("❌ [ProviderRepository] Ошибка обновления дня напоминания: ${e.message}")
            e.printStackTrace()
            throw e
        }
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
            val provider = providerDao.getById(providerId).first()

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

    /**
     * Получить провайдеров, у которых сегодня период передачи показаний
     */
    suspend fun getProvidersInTransmissionPeriod(): List<ProviderDomainModel> {
        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        return providerDao.getAll().first()
            .filter { provider ->
                val startDay = provider.transmissionPeriodStartDay
                val endDay = provider.transmissionPeriodEndDay
                startDay != null && endDay != null && todayDay in startDay..endDay
            }
            .map { it.toDomain() }
    }
}
