package ru.dr.meterreadings.domain.exceptions

class AccountNotFoundException(
    message: String
) : RuntimeException(message)