package ru.dr.meterreadings.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.dr.meterreadings.models.domain.AuthTokenDomainModel
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Сохранить токен
     */
    fun saveToken(token: AuthTokenDomainModel) {
        val key = "${token.providerId}_${token.login}"
        val tokenJson = json.encodeToString(token)

        encryptedPrefs.edit()
            .putString(key, tokenJson)
            .apply()

        println("✅ [TokenManager] Токен сохранён: providerId=${token.providerId}, login=${token.login}")
    }

    /**
     * Получить токен
     */
    fun getToken(providerId: Long, login: String): AuthTokenDomainModel? {
        val key = "${providerId}_${login}"
        val tokenJson = encryptedPrefs.getString(key, null) ?: return null

        return try {
            json.decodeFromString<AuthTokenDomainModel>(tokenJson)
        } catch (e: Exception) {
            println("❌ [TokenManager] Ошибка десериализации: ${e.message}")
            null
        }
    }

    /**
     * Получить все токены для провайдера
     */
    fun getAllTokensForProvider(providerId: Long): List<AuthTokenDomainModel> {
        val prefix = "${providerId}_"

        return encryptedPrefs.all
            .filterKeys { it.startsWith(prefix) }
            .mapNotNull { (_, value) ->
                try {
                    json.decodeFromString<AuthTokenDomainModel>(value as String)
                } catch (e: Exception) {
                    null
                }
            }
    }

    /**
     * Удалить токен
     */
    fun deleteToken(providerId: Long, login: String) {
        val key = "${providerId}_${login}"
        encryptedPrefs.edit().remove(key).apply()
        println("🗑️ [TokenManager] Токен удалён: $login")
    }

    /**
     * Проверить, истёк ли access token
     */
    fun isAccessTokenExpired(token: AuthTokenDomainModel): Boolean {
        return try {
            // ✅ ИСПРАВЛЕНО: создаём Instant из миллисекунд
            val expiresAt = Instant.ofEpochMilli(token.accessTokenExpiresAt)
            val now = Instant.now()
            val buffer = java.time.Duration.ofMinutes(5)

            // Проверяем: истекает ли токен в ближайшие 5 минут?
            expiresAt.isBefore(now.plus(buffer))
        } catch (e: Exception) {
            println("⚠️ [TokenManager] Ошибка проверки токена: ${e.message}")
            true // В случае ошибки считаем токен истёкшим
        }
    }

    /**
     * Проверить, истёк ли refresh token
     */
    fun isRefreshTokenExpired(token: AuthTokenDomainModel): Boolean {
        return try {
            val expiresAt = Instant.ofEpochMilli(token.refreshTokenExpiresAt)
            val now = Instant.now()

            // ✅ БЕЗ буфера! Проверяем: УЖЕ истёк?
            expiresAt.isBefore(now)
        } catch (e: Exception) {
            println("⚠️ [TokenManager] Ошибка проверки refresh token: ${e.message}")
            true // В случае ошибки считаем истёкшим
        }
    }

    /**
     * Обновить access token (после refresh)
     */
    fun updateAccessToken(
        providerId: Long,
        login: String,
        newAccessToken: String,
        newAccessTokenExpires: Long
    ) {
        val token = getToken(providerId, login) ?: return

        val updated = token.copy(
            accessToken = newAccessToken,
            accessTokenExpiresAt = newAccessTokenExpires
        )

        saveToken(updated)
        println("🔄 [TokenManager] Access token обновлён для $login")
    }

    /**
     * Сохранить токен капчи для провайдера и аккаунта
     */
    fun saveCaptchaToken(
        providerId: Long,
        accountNumber: String,
        captchaToken: String
    ) {
        val key = "$providerId:captcha:$accountNumber"

        encryptedPrefs.edit()
            .putString(key, captchaToken)  // ✅ Просто токен, БЕЗ expiration!
            .apply()

        println("TokenManager: Captcha token saved for provider $providerId, account $accountNumber")
    }

    /**
     * Получить токен капчи
     */
    fun getCaptchaToken(
        providerId: Long,
        accountNumber: String
    ): String? {
        val key = "$providerId:captcha:$accountNumber"
        return encryptedPrefs.getString(key, null)
    }

    /**
     * Удалить токен капчи (при ошибке от сервера)
     */
    fun deleteCaptchaToken(
        providerId: Long,
        accountNumber: String
    ) {
        val key = "$providerId:captcha:$accountNumber"
        encryptedPrefs.edit().remove(key).apply()
        println("TokenManager: Captcha token deleted for account $accountNumber")
    }
}
