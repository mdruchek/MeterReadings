package ru.dr.meterreadings.domain.exceptions

class MeterNotFoundException(
    message: String
) : RuntimeException(message)