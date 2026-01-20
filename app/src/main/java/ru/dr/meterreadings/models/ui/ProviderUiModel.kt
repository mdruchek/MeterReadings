package ru.dr.meterreadings.models.ui

import ru.dr.meterreadings.models.domain.ProviderDomainModel

/**
 * UI-модель провайдера для отображения на экранах.
 *
 * Содержит Domain модель + UI-специфичные состояния.
 */
data class ProviderUiModel(
    val provider: ProviderDomainModel,

    // ============================================
    // UI-СПЕЦИФИЧНЫЕ СОСТОЯНИЯ
    // ============================================

    /** Раскрыта ли карточка в списке */
    val isExpanded: Boolean = false,

    /** Идёт ли загрузка для этого провайдера */
    val isLoading: Boolean = false
)

// ========================================
// МАППИНГ: Domain → UI
// ========================================

/**
 * Конвертирует DomainModel в UiModel.
 */
fun ProviderDomainModel.toUiModel(): ProviderUiModel {
    return ProviderUiModel(
        provider = this,
        isExpanded = false,
        isLoading = false
    )
}
