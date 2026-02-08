package ru.dr.meterreadings.utils

import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException
import ru.dr.meterreadings.domain.service.CaptchaService
import ru.dr.meterreadings.ui.components.CaptchaSession

/**
 * Обёртка для сетевых запросов КВЦ с автоматической проверкой капчи
 *
 * Комбинирует проверку капчи + обработку HTTP ошибок + очистку сессии при ошибке.
 *
 * @param captchaService Сервис капчи
 * @param accountNumber Номер лицевого счёта
 * @param block Блок с сетевым запросом, принимает captchaSession
 * @return Result с данными или CaptchaRequiredException/NetworkException
 */
suspend fun <T> safeKvcCall(
    captchaService: CaptchaService,
    providerId: Long,
    accountNumber: String,
    block: suspend (session: CaptchaSession) -> T
): Result<T> {
    return try {
        val session = captchaService.getCaptchaSession(providerId, accountNumber)
            ?: return Result.failure(CaptchaRequiredException("Требуется пройти проверку капчи"))

        println("🔐 [KvcCall] Используем сессию капчи для provider=$providerId, account=$accountNumber")

        safeNetworkCall {
            try {
                block(session)
            } catch (e: CaptchaRequiredException) {
                println("🗑️ [KvcCall] Капча невалидна - очищаем сессию")
                captchaService.clearCaptchaSession(providerId, accountNumber)
                throw e  // Пробрасываем дальше
            }
        }.also { result ->
            if (result.isSuccess) {
                println("✅ [KvcCall] Запрос успешен")
            }
        }

    } catch (e: Exception) {
        println("❌ [KvcCall] Ошибка: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}

/**
 * Упрощённая версия без проверки капчи (для getRegions и т.п.)
 */
suspend fun <T> safeKvcCallNoAuth(
    block: suspend () -> T
): Result<T> {
    return safeNetworkCall { block() }
}
