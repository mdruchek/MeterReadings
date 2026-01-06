package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.ProfileDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.ProfileDomainModel
import java.util.UUID
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
 * Отвечает за:
 * - CRUD операции с профилями
 * - Преобразование Entity ↔ Domain
 * - Бизнес-логику (генерация ID, валидация)
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,  // Hilt передаст автоматически
    private val accountRepository: AccountRepository
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
    fun getAllProfiles(): Flow<List<ProfileDomainModel>> {
        return profileDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Получить профиль по ID как Flow (с автообновлением)
     */
    fun getProfileById(id: String): Flow<ProfileDomainModel?> {
        return profileDao.getById(id)
            .map { entity -> entity?.toDomain() }
    }

    // ========================================
    // ЗАПИСЬ (WRITE)
    // ========================================

    /**
     * Добавить новый профиль
     *
     * Генерирует UUID, проверяет дубликаты, сохраняет в БД
     * Возвращает ID созданного профиля
     */
    suspend fun createProfile(name: String, icon: String? = null): String {
        // Проверка: не существует ли профиль с таким именем
        val exists = profileDao.existsByName(name)
        if (exists) {
            throw IllegalArgumentException("Profile with name '$name' already exists")
        }

        // Создаём DomainModel с новым ID
        val domainModel = ProfileDomainModel(
            id = UUID.randomUUID().toString(),  // Генерируем уникальный ID
            name = name,
            icon = icon
        )

        // Конвертируем в Entity и сохраняем
        val entity = domainModel.toEntity(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        profileDao.insert(entity)

        return domainModel.id  // Возвращаем ID для навигации
    }

    /**
     * Обновить профиль (универсальный метод)
     *
     * Использует существующий createdAt, обновляет остальные поля
     * Для изменения только имени удобнее использовать updateProfileName()
     */
    suspend fun updateProfile(profile: ProfileDomainModel) {
        // Получаем текущий профиль для сохранения createdAt
        val existingEntity = profileDao.getById(profile.id).first()
            ?: throw IllegalArgumentException("Profile not found")

        // Создаём обновлённую Entity
        val updatedEntity = profile.toEntity(
            createdAt = existingEntity.createdAt,  // Сохраняем старое время создания
            updatedAt = System.currentTimeMillis()  // Новое время обновления
        )

        profileDao.update(updatedEntity)
    }

    /**
     * Обновить имя профиля (быстрый метод с валидацией)
     *
     * Проверяет уникальность имени
     * Для универсального обновления используйте updateProfile()
     */
    suspend fun updateProfileName(profileId: String, newName: String) {
        // Получаем текущий профиль
        val entity = profileDao.getById(profileId).first()
            ?: throw IllegalArgumentException("Profile not found")

        // Валидация: проверяем что новое имя не занято ДРУГИМ профилем
        val nameExists = profileDao.existsByName(newName)
        if (nameExists && entity.name != newName) {
            // nameExists - имя существует в БД
            // entity.name != newName - это ДРУГОЙ профиль (не текущий)
            throw IllegalArgumentException("Profile with name '$newName' already exists")
        }

        // Обновляем только name и updatedAt
        val updated = entity.copy(
            name = newName,
            updatedAt = System.currentTimeMillis()
            // id, createdAt - остаются прежними (copy не указали)
        )

        profileDao.update(updated)
    }

    // ========================================
    // УДАЛЕНИЕ (DELETE)
    // ========================================

    /**
     * Удалить профиль
     *
     * Также удаляет все связанные accounts благодаря CASCADE в ForeignKey
     * (при удалении профиля Room автоматически удалит все его аккаунты)
     */
    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteById(profileId)
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
     * Получить количество профилей
     */
    suspend fun getProfileCount(): Int {
        return profileDao.getCount()
    }

    /**
     * Проверить существование профиля с таким именем
     *
     * Используется для валидации перед добавлением/обновлением
     */
    suspend fun profileExists(name: String): Boolean {
        return profileDao.existsByName(name)
    }

    /**
     * Получить количество аккаунтов в профиле
     *
     * Делегирует запрос в AccountRepository
     */
    suspend fun getAccountCountForProfile(profileId: String): Int {
        return accountRepository.getAccountCount(profileId)
    }

}
