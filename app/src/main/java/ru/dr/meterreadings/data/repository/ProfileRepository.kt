package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.ProfileDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с профилями
 *
 * Скрывает источник данных от ViewModel.
 * Сейчас данные только из БД (Room), позже можно добавить API.
 *
 * @Inject - Hilt автоматически создаст и передаст ProfileDao
 * @Singleton - один экземпляр на всё приложение
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao  // Hilt передаст автоматически
) {

    // ========================================
    // ЧТЕНИЕ (READ)
    // ========================================

    /**
     * Получить все профили как Flow (с автообновлением)
     *
     * Flow<List<ProfileEntity>> из DAO
     *   ↓ map { list -> ... }
     * Flow<List<ProfileDomainModel>> в ViewModel
     *
     * Когда БД изменится → Flow автоматически обновится → UI перерисуется!
     */
    fun getAllProfilesFlow(): Flow<List<ProfileDomainModel>> {
        return profileDao.getAllFlow()
            .map { entities ->  // List<ProfileEntity>
                entities.map { it.toDomain() }  // List<ProfileDomainModel>
            }
    }

    /**
     * Получить все профили (одноразово, без автообновления)
     *
     * suspend - выполняется асинхронно в корутине
     */
    suspend fun getAllProfiles(): List<ProfileDomainModel> {
        val entities = profileDao.getAll()  // List<ProfileEntity> из БД
        return entities.map { it.toDomain() }  // Конвертируем в DomainModel
    }

    /**
     * Получить профиль по ID
     *
     * Возвращает null если профиль не найден
     */
    suspend fun getProfileById(id: String): ProfileDomainModel? {
        val entity = profileDao.getById(id)  // ProfileEntity? из БД
        return entity?.toDomain()  // Конвертируем в DomainModel (или null)
    }

    /**
     * Получить профиль по умолчанию
     */
    suspend fun getDefaultProfile(): ProfileDomainModel? {
        val entity = profileDao.getDefault()
        return entity?.toDomain()
    }

    /**
     * Получить профиль по ID как Flow (с автообновлением)
     */
    fun getProfileByIdFlow(id: String): Flow<ProfileDomainModel?> {
        return profileDao.getByIdFlow(id)
            .map { entity -> entity?.toDomain() }
    }

    // ========================================
    // ЗАПИСЬ (WRITE)
    // ========================================

    /**
     * Сохранить новый профиль или обновить существующий
     *
     * ProfileDomainModel (из ViewModel)
     *   ↓ toEntity()
     * ProfileEntity → вставка в БД
     */
    suspend fun saveProfile(profile: ProfileDomainModel) {
        println("💾 Сохраняем профиль: ${profile.name}")
        val entity = profile.toEntity(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        profileDao.insert(entity)  // Вставка в БД
        println("✅ Профиль сохранён!")
    }

    /**
     * Обновить существующий профиль
     *
     * Нужно сначала получить из БД (для createdAt), потом обновить
     */
    suspend fun updateProfile(profile: ProfileDomainModel) {
        // Получаем старую запись из БД
        val oldEntity = profileDao.getById(profile.id)

        if (oldEntity != null) {
            // Создаём обновлённую Entity с новым updatedAt
            val updatedEntity = profile.toEntity(
                createdAt = oldEntity.createdAt,  // Сохраняем старую дату создания
                updatedAt = System.currentTimeMillis()  // Новая дата обновления
            )
            profileDao.update(updatedEntity)
        } else {
            // Если профиль не найден - создаём новый
            saveProfile(profile)
        }
    }

    // ========================================
    // УДАЛЕНИЕ (DELETE)
    // ========================================

    /**
     * Удалить профиль по ID
     */
    suspend fun deleteProfile(id: String) {
        profileDao.deleteById(id)
    }

    /**
     * Удалить все профили (для тестов или сброса)
     */
    suspend fun deleteAllProfiles() {
        profileDao.deleteAll()
    }

    // ========================================
    // СПЕЦИАЛЬНЫЕ ОПЕРАЦИИ
    // ========================================

    /**
     * Установить профиль по умолчанию
     *
     * @Transaction в DAO гарантирует атомарность:
     * - Сначала сбросит isDefault у всех
     * - Потом установит у нужного
     */
    suspend fun setDefaultProfile(profileId: String) {
        profileDao.setDefault(profileId)
    }

    /**
     * Получить количество профилей
     */
    suspend fun getProfileCount(): Int {
        return profileDao.getCount()
    }
}
