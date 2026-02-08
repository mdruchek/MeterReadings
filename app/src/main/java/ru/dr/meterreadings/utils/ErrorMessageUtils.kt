package ru.dr.meterreadings.utils

import ru.dr.meterreadings.data.remote.dto.kvc.CaptchaRequiredException

/**
 * Преобразует техническую ошибку в понятное пользователю сообщение
 *
 * Используется для ошибок НЕ связанных с сетью (например, сериализация)
 */
fun Throwable.toUserFriendlyMessage(): String {
    val message = this.message ?: "Неизвестная ошибка"

    // Логируем полную ошибку для разработчика
    println("❌ [ErrorHandler] ${this.javaClass.simpleName}: $message")
    this.printStackTrace()

    // Возвращаем понятное сообщение
    return when {
        this is CaptchaRequiredException -> message

        // Сериализация (внутренние ошибки кода)
        message.contains("Serializer for class", ignoreCase = true) ->
            "Ошибка обработки данных"

        message.contains("@Serializable", ignoreCase = true) ->
            "Ошибка обработки данных"

        message.contains("reflection is not available", ignoreCase = true) ->
            "Ошибка обработки данных"

        // База данных
        message.contains("UNIQUE constraint", ignoreCase = true) ->
            "Запись уже существует"

        message.contains("NOT NULL constraint", ignoreCase = true) ->
            "Не все обязательные поля заполнены"

        // Общие ошибки
        message.contains("IllegalArgumentException", ignoreCase = true) ->
            "Некорректные данные"

        message.contains("NullPointerException", ignoreCase = true) ->
            "Отсутствуют необходимые данные"

        // Если уже человеческое сообщение
        else -> message
    }
}
