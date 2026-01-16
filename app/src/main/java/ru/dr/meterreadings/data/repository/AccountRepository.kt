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
    fun getAccountById(accountId: String): Flow<AccountDomainModel?> {
        return accountDao.getById(accountId).map { it?.toDomain() }
    }

    /**
     * Получить все accounts конкретного провайдера
     */
    fun getAccountsByProviderId(providerId: String): Flow<List<AccountDomainModel>> {
        return accountDao.getByProviderId(providerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Добавить новый account
     * Возвращает ID созданного account
     */
    suspend fun addAccount(
        profileId: String,
        providerId: String,
        accountNumber: String,
        regionId: Int? = null,
        login: String? = null,
        password: String? = null
    ): String {
        println("💾 [AccountRepository] НАЧАЛО addAccount")
        println("   profileId: $profileId")
        println("   providerId: $providerId")
        println("   accountNumber: $accountNumber")
        println("   regionId: $regionId")

        val exists = accountDao.exists(profileId, accountNumber)
        if (exists) {
            throw IllegalArgumentException("Account with number $accountNumber already exists for this profile")
        }

        val domainModel = AccountDomainModel(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            providerId = providerId,
            accountNumber = accountNumber,
            regionId = regionId
        )

        val entity = domainModel.toEntity(
            login = login,
            password = password,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        accountDao.insert(entity)
        println("✅ [Repository] Аккаунт добавлен в БД: ${entity.id}")
        println("   regionId в БД: ${entity.regionId}")
        return domainModel.id
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
