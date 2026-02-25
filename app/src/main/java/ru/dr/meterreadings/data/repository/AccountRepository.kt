package ru.dr.meterreadings.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.dr.meterreadings.data.local.dao.AccountDao
import ru.dr.meterreadings.data.local.entities.toDomain
import ru.dr.meterreadings.data.local.entities.toEntity
import ru.dr.meterreadings.models.domain.AccountDomainModel
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для работы с аккаунтами (лицевыми счетами)
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {

    /**
     * Получить все accounts профиля (Flow - автообновление)
     */
    fun getAccountsByProfileId(profileId: String): Flow<List<AccountDomainModel>> {
        return accountDao.getByProfileId(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Получить все аккаунты (для Worker и фоновых задач)
     */
    fun getAllAccounts(): Flow<List<AccountDomainModel>> {
        return accountDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Получить account по ID
     */
    fun getAccountByIdFlow(accountId: String): Flow<AccountDomainModel?> {
        return accountDao.getById(accountId).map { it?.toDomain() }
    }

    /**
     * Получить account по ID (suspend-версия для одноразового запроса)
     */
    suspend fun getAccountByIdOnce(accountId: String): AccountDomainModel {
        return accountDao.getById(accountId).first()?.toDomain()
            ?: throw NoSuchElementException("Account with id $accountId not found")
    }

    /**
     * Получить account по номеру лицевого счёта
     */
    suspend fun findByAccountNumber(accountNumber: String): AccountDomainModel? {
        return accountDao.findByAccountNumber(accountNumber)?.toDomain()
    }


    /**
     * Получить все accounts конкретного провайдера
     */
    fun getAccountsByProviderId(providerId: String): Flow<List<AccountDomainModel>> {
        return accountDao.getByProviderId(providerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // ========================================
    // ЗАПИСЬ (WRITE)
    // ========================================

    /**
     * Добавить новый аккаунт
     *
     * ✅ Принимает AccountDomainModel (из ViewModel)
     * ✅ Проверяет дубликаты
     * ✅ Сохраняет в БД
     *
     * @param account Domain модель аккаунта
     * @return ID созданного аккаунта
     */
    suspend fun addAccount(account: AccountDomainModel): String {
        println("💾 [AccountRepository] Добавление аккаунта")
        println("   profileId: ${account.profileId}")
        println("   providerId: ${account.providerId}")
        println("   number: ${account.number}")
        println("   uuid: ${account.uuid}")
        println("   regionId: ${account.regionId}")
        println("   login: ${account.login}")

        // Проверка дубликатов
        val exists = accountDao.exists(account.profileId, account.number)
        if (exists) {
            throw IllegalArgumentException(
                "Аккаунт ${account.number} уже существует для этого профиля"
            )
        }

        // Преобразуем AccountDomainModel → AccountEntity
        val entity = account.toEntity()

        // Сохраняем в БД
        accountDao.insert(entity)

        println("✅ [AccountRepository] Аккаунт сохранён: ${entity.id}")
        return entity.id
    }

    /**
     * Обновить credentials (логин/пароль) account
     */
    suspend fun updateCredentials(
        accountId: String,
        login: String?,
        password: String?
    ) {
        val entity = accountDao.getById(accountId).first()
            ?: throw IllegalArgumentException("Account not found")

        val updated = entity.copy(
            login = login,
            password = password,
            updatedAt = System.currentTimeMillis()
        )

        accountDao.update(updated)
    }

    suspend fun updateAccount(model: AccountDomainModel) {
        val existing = accountDao.getById(model.id).first()
            ?: throw IllegalArgumentException("Аккаунт не найден")
        val updated = model.toEntity(
            login = existing.login,
            password = existing.password,
            createdAt = existing.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        accountDao.update(updated)
        println("✅ [AccountRepository] Аккаунт обновлён: ${model.number}, uuid=${model.uuid}")
    }

    /**
     * Удалить account
     */
    suspend fun deleteAccount(accountId: String) {
        accountDao.deleteById(accountId)
    }

    /**
     * Удалить все accounts профиля
     */
    suspend fun deleteAccountsByProfileId(profileId: String) {
        accountDao.deleteByProfileId(profileId)
    }

    /**
     * Получить количество accounts у профиля
     */
    suspend fun getAccountCount(profileId: String): Int {
        return accountDao.getCountByProfileId(profileId)
    }

    /**
     * Проверить существование account с таким номером
     */
    suspend fun accountExists(profileId: String, accountNumber: String): Boolean {
        return accountDao.exists(profileId, accountNumber)
    }
}
