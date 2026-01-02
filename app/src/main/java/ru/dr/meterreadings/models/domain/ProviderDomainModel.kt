package ru.dr.meterreadings.models.domain

data class ProviderDomainModel(
    val id: String,
    val name: String,
    val type: String,
    val logoUrl: String? = null,   // URL
    val baseUrl: String,
    val authType6: AuthType
)

enum class AuthType {
    API_KEY,
    FORM_CSRF,
    AUTH_REQUIRED
}
