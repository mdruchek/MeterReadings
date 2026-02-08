package ru.dr.meterreadings.domain.service

import ru.dr.meterreadings.ui.components.CaptchaSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptchaService @Inject constructor() {
    private val sessions = mutableMapOf<String, CaptchaSession>()

    // ✅ Формируем уникальный ключ из providerId + accountNumber
    private fun getKey(providerId: Long, accountNumber: String): String {
        return "${providerId}_$accountNumber"
    }

    fun saveCaptchaSession(providerId: Long, accountNumber: String, session: CaptchaSession) {
        val key = getKey(providerId, accountNumber)
        sessions[key] = session
        println("CaptchaService: saved session for provider=$providerId, account=$accountNumber")
        println("  Token: ${session.token.take(80)}...")
        println("  Cookies: ${session.cookies.take(100)}...")
        println("  User-Agent: ${session.userAgent}")
    }

    fun getCaptchaSession(providerId: Long, accountNumber: String): CaptchaSession? {
        val key = getKey(providerId, accountNumber)
        return sessions[key]
    }

    fun hasCaptchaSession(providerId: Long, accountNumber: String): Boolean {
        val key = getKey(providerId, accountNumber)
        return sessions.containsKey(key)
    }

    fun clearCaptchaSession(providerId: Long, accountNumber: String) {
        val key = getKey(providerId, accountNumber)
        sessions.remove(key)
        println("CaptchaService: cleared session for provider=$providerId, account=$accountNumber")
    }
}
