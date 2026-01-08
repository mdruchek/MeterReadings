package ru.dr.meterreadings.models.domain

data class ProviderDomainModel(
    val id: String,
    val name: String,
    val type: Type,
    val logoUrl: String? = null,   // URL
    val baseUrl: String,
    val authType: AuthType
)

enum class AuthType {
    API_KEY,
    FORM_CSRF,
    AUTH_REQUIRED
}

enum class Type {
    WaterSupply,
    ElectricitySupply,
    GasSupply
}
