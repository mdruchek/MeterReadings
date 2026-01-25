// app/src/main/java/ru/dr/meterreadings/data/repository/ProfileRepository.kt

package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.ProfileDao
import ru.dr.meterreadings.data.local.dao.ProviderDao
import ru.dr.meterreadings.data.local.entities.ProfileEntity
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.ui.ProfileUiModel
// ✅ ЗАМЕНА java.time.* на старые классы
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val providerDao: ProviderDao
) {

    // ============================================
    // ПРОФИЛИ
    // ============================================

    /**
     * Получить все профили с количеством аккаунтов (Flow)
     */
    fun getProfilesWithAccountCount(): Flow<List<ProfileUiModel>> {
        return profileDao.getProfilesWithAccountCount()
            .map { list ->
                list.map { entity ->
                    ProfileUiModel(
                        profile = entity.profile.toDomain(),
                        accountCount = entity.accountCount,
                        addressCount = 0, // TODO: реализовать подсчёт
                        readingsCount = 0, // TODO: реализовать подсчёт
                        lastUpdateDate = null // TODO: реализовать
                    )
                }
            }
    }

    /**
     * Получить профиль по ID (Flow)
     */
    fun getProfileById(profileId: String): Flow<ProfileDomainModel?> {
        return profileDao.getById(profileId)
            .map { it?.toDomain() }
    }

    /**
     * Создать новый профиль
     */
    suspend fun createProfile(name: String, icon: String? = null): String {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val profile = ProfileEntity(
            id = id,
            name = name,
            icon = icon,
            isDefault = false,
            createdAt = now,
            updatedAt = now
        )

        profileDao.insert(profile)
        println("✅ [ProfileRepository] Профиль создан: $name (ID: $id)")
        return id
    }

    /**
     * Обновить имя профиля
     */
    suspend fun updateProfileName(profileId: String, newName: String) {
        val profile = profileDao.getById(profileId).first()
        if (profile != null) {
            val updated = profile.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )

            profileDao.update(updated)
            println("✅ [ProfileRepository] Профиль обновлён: $newName")
        }
    }

    /**
     * Обновить иконку профиля
     */
    suspend fun updateProfileIcon(profileId: String, newIcon: String) {
        val profile = profileDao.getById(profileId).first()
        if (profile != null) {
            val updated = profile.copy(
                icon = newIcon,
                updatedAt = System.currentTimeMillis()
            )

            profileDao.update(updated)
            println("✅ [ProfileRepository] Иконка профиля обновлена: $newIcon")
        } else {
            throw IllegalArgumentException("Profile not found")
        }
    }

    /**
     * Удалить профиль
     * Связанные аккаунты удаляются автоматически (CASCADE)
     */
    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteById(profileId)
        println("✅ [ProfileRepository] Профиль удалён: $profileId")
    }

    // ============================================
    // ПРОВАЙДЕРЫ
    // ============================================

    /**
     * Получить всех провайдеров (Flow)
     */
    fun getAllProviders(): Flow<List<ProviderDomainModel>> {
        return providerDao.getAll()
            .map { list -> list.map { it.toDomain() } }
    }

    /**
     * Получить провайдера по ID (Flow)
     */
    fun getProviderById(providerId: Long): Flow<ProviderDomainModel?> {
        return providerDao.getById(providerId)
            .map { it?.toDomain() }
    }

    /**
     * Обновить период передачи показаний для провайдера
     */
    suspend fun updateProviderTransmissionPeriod(
        providerId: Long,
        periodStartDay: Int,
        periodEndDay: Int
    ) {
        try {
            val provider = providerDao.getById(providerId).first()
            if (provider != null) {
                // ✅ ИСПОЛЬЗУЕМ SimpleDateFormat вместо LocalDate
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                val updated = provider.copy(
                    transmissionPeriodStartDay = periodStartDay,
                    transmissionPeriodEndDay = periodEndDay,
                    lastPeriodUpdate = System.currentTimeMillis(),
                    periodLoadedForMonth = currentMonth,
                    updatedAt = System.currentTimeMillis()
                )

                providerDao.update(updated)
                println("✅ [ProfileRepository] Период передачи обновлён для провайдера $providerId")
                println("   Дни: $periodStartDay - $periodEndDay")
                println("   Месяц: $currentMonth")
            } else {
                println("⚠️ [ProfileRepository] Провайдер $providerId не найден")
            }

        } catch (e: Exception) {
            println("❌ [ProfileRepository] Ошибка обновления периода: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Получить провайдеров, у которых сегодня период передачи показаний
     */
    suspend fun getProvidersInTransmissionPeriod(): List<ProviderDomainModel> {
        // ✅ ИСПОЛЬЗУЕМ Calendar вместо LocalDate
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

    /**
     * Обновить настройки провайдера
     */
    suspend fun updateProviderSettings(
        providerId: Long,
        autoUpdateEnabled: Boolean,
        updateStartDay: Int,
        updateIntervalHours: Int,
        updateNotificationsEnabled: Boolean,
        errorNotificationsEnabled: Boolean,
        reminderEnabled: Boolean,
        reminderTimeHour: Int,
        reminderTimeMinute: Int,
        reminderPeriodMode: String,
        reminderCustomStartDay: Int?,
        reminderCustomEndDay: Int?
    ) {
        try {
            val provider = providerDao.getById(providerId).first()
            if (provider != null) {
                val updated = provider.copy(
                    autoUpdateEnabled = autoUpdateEnabled,
                    notificationsEnabled = updateNotificationsEnabled,
                    reminderEnabled = reminderEnabled,
                    reminderCustomStartDay = reminderCustomStartDay,
                    updatedAt = System.currentTimeMillis()
                )

                providerDao.update(updated)
                println("✅ [ProfileRepository] Настройки провайдера обновлены")
                println("   Автообновление: $autoUpdateEnabled")
                println("   День начала: $updateStartDay")
                println("   Интервал: $updateIntervalHours ч")
                println("   Напоминания: $reminderEnabled в $reminderTimeHour:$reminderTimeMinute")
            } else {
                println("⚠️ [ProfileRepository] Провайдер $providerId не найден")
            }

        } catch (e: Exception) {
            println("❌ [ProfileRepository] Ошибка обновления настроек: ${e.message}")
            e.printStackTrace()
        }
    }
}
