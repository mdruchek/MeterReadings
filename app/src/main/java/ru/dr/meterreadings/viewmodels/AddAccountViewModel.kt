package ru.dr.meterreadings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.dr.meterreadings.data.repository.AccountRepository
import ru.dr.meterreadings.data.repository.ProviderRepository
import ru.dr.meterreadings.domain.connector.GetRegions  // ← ДОБАВИТЬ
import ru.dr.meterreadings.domain.connector.GetAccounts  // ← ДОБАВИТЬ
import ru.dr.meterreadings.domain.connector.ProviderConnectorFactory
import ru.dr.meterreadings.domain.connector.AppAuth
import ru.dr.meterreadings.domain.connector.UserAuth
import ru.dr.meterreadings.models.domain.ProviderDomainModel
import ru.dr.meterreadings.models.ui.ProviderUiModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val providerRepository: ProviderRepository,
    private val connectorFactory: ProviderConnectorFactory
) : ViewModel() {

    // =====================================================
    // МОДЕЛЬ ОШИБКИ
    // =====================================================

    data class ErrorState(
        val title: String,
        val message: String
    )

    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

    private val _shouldResetToStep1 = MutableStateFlow(false)
    val shouldResetToStep1: StateFlow<Boolean> = _shouldResetToStep1.asStateFlow()

    fun showError(title: String, message: String) {
        _errorState.value = ErrorState(title, message)
        _shouldResetToStep1.value = true
        println("❌ [AddAccountVM] Ошибка: $title - $message")
    }

    fun dismissError() {
        _errorState.value = null
    }

    fun resetCompleted() {
        _shouldResetToStep1.value = false
        println("🔄 [AddAccountVM] Сброс завершён")
    }

    // =====================================================
    // STATE - поисковый запрос
    // =====================================================

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        println("🔍 [AddAccountVM] Поиск: '$query'")
    }

    // =====================================================
    // STATE - список провайдеров (с фильтрацией)
    // =====================================================

    private val allProviders: StateFlow<List<ProviderDomainModel>> =
        providerRepository.getAllProviders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val filteredProviders: StateFlow<List<ProviderUiModel>> = combine(
        allProviders,
        searchQuery
    ) { providers, query ->
        println("🔄 [AddAccountVM] Фильтрация: ${providers.size} провайдеров, запрос: '$query'")
        if (query.isBlank()) {
            providers.map { ProviderUiModel(it) }
        } else {
            val filtered = providers.filter { provider ->
                provider.name.contains(query, ignoreCase = true)
            }
            println("✅ [AddAccountVM] Найдено: ${filtered.size}")
            filtered.map { ProviderUiModel(it) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // =====================================================
    // STATE - выбранный провайдер
    // =====================================================

    private val _selectedProviderId = MutableStateFlow<Long?>(null)
    val selectedProviderId: StateFlow<Long?> = _selectedProviderId.asStateFlow()

    fun selectProvider(providerId: Long) {
        _selectedProviderId.value = providerId
        println("✅ [AddAccountVM] Выбран провайдер: $providerId")
    }

    fun getSelectedProvider(): StateFlow<ProviderDomainModel?> {
        return combine(allProviders, selectedProviderId) { providers, selectedId ->
            if (selectedId == null) null
            else providers.find { it.id == selectedId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun clearSelection() {
        _selectedProviderId.value = null
        _regions.value = emptyList()
        _selectedRegionId.value = null
        _providerHasRegions.value = false
        println("🔄 [AddAccountVM] Выбор сброшен")
    }

    // =====================================================
    // STATE - регионы провайдера
    // =====================================================

    private val _regions = MutableStateFlow<List<GetRegions.RegionInfo>>(emptyList())
    val regions: StateFlow<List<GetRegions.RegionInfo>> = _regions.asStateFlow()

    private val _providerHasRegions = MutableStateFlow(false)
    val providerHasRegions: StateFlow<Boolean> = _providerHasRegions.asStateFlow()

    private val _isLoadingRegions = MutableStateFlow(false)
    val isLoadingRegions: StateFlow<Boolean> = _isLoadingRegions.asStateFlow()

    /**
     * Авторизация приложения на API провайдера (если требуется)
     *
     * Вызывается перед loadRegionsForProvider для провайдеров,
     * которые требуют авторизации приложения (например, ТНС)
     */
    private suspend fun authorizeAppIfNeeded(providerId: Long): Boolean {
        return try {
            val connector = connectorFactory.getConnector(providerId)

            if (connector is AppAuth) {
                println("🔐 [AddAccountVM] Требуется авторизация приложения...")
                val result = connector.appAuth()

                result.onSuccess { isAuthorized ->
                    if (isAuthorized) {
                        println("✅ [AddAccountVM] Приложение авторизовано")
                        return true
                    } else {
                        println("❌ [AddAccountVM] Авторизация не удалась")
                        showError(
                            title = "Ошибка авторизации",
                            message = "Не удалось авторизовать приложение на сервере"
                        )
                        return false
                    }
                }.onFailure { error ->
                    println("❌ [AddAccountVM] Ошибка авторизации: ${error.message}")
                    showError(
                        title = "Ошибка авторизации",
                        message = error.message ?: "Неизвестная ошибка"
                    )
                    return false
                }
            }

            // Провайдер не требует авторизации приложения
            true
        } catch (e: Exception) {
            println("❌ [AddAccountVM] Исключение при авторизации: ${e.message}")
            showError(
                title = "Ошибка",
                message = e.message ?: "Неизвестная ошибка"
            )
            false
        }
    }

    /**
     * Загрузка регионов для выбранного провайдера
     *
     * ✅ Вызывается из UI при переходе на шаг 2
     */
    fun loadRegionsForProvider(providerId: Long) {  // ← Убрали private
        viewModelScope.launch {
            _isLoadingRegions.value = true
            _regions.value = emptyList()

            try {
                if (!authorizeAppIfNeeded(providerId)) {
                    println("❌ [AddAccountVM] Авторизация не прошла, прерываем загрузку регионов")
                    return@launch
                }

                val connector = connectorFactory.getConnector(providerId)

                if (connector is GetRegions) {
                    println("🔍 [AddAccountVM] Провайдер поддерживает регионы, загружаем...")

                    val result = connector.getRegions()

                    result.onSuccess { regionsList ->
                        _regions.value = regionsList
                        _providerHasRegions.value = regionsList.isNotEmpty()
                        println("✅ [AddAccountVM] Загружено регионов: ${regionsList.size}")
                    }.onFailure { error ->
                        handleLoadRegionsError(error)
                    }
                } else {
                    println("ℹ️ [AddAccountVM] Провайдер НЕ поддерживает регионы")
                    _providerHasRegions.value = false
                }
            } catch (e: Exception) {
                handleLoadRegionsError(e)
            } finally {
                _isLoadingRegions.value = false
            }
        }
    }

    private fun handleLoadRegionsError(error: Throwable) {
        val message = when (error) {
            is SocketTimeoutException ->
                "Сервер не отвечает. Проверьте подключение к интернету или попробуйте позже."

            is ConnectException, is UnknownHostException ->
                "Не удалось подключиться к серверу. Проверьте подключение к интернету."

            is IOException ->
                "Ошибка сети: ${error.message ?: "Неизвестная ошибка"}"

            else ->
                "Не удалось загрузить данные: ${error.message ?: "Неизвестная ошибка"}"
        }

        showError(
            title = "Ошибка подключения",
            message = message
        )
    }

    // =====================================================
    // STATE - выбранный регион
    // =====================================================

    private val _selectedRegionId = MutableStateFlow<String?>(null)
    val selectedRegionId: StateFlow<String?> = _selectedRegionId.asStateFlow()

    fun selectRegion(regionId: String) {
        _selectedRegionId.value = regionId
        println("✅ [AddAccountVM] Выбран регион: $regionId")
    }

    // =====================================================
// АВТОРИЗАЦИЯ ПОЛЬЗОВАТЕЛЯ (для ТНС)
// =====================================================

    private val _authData = MutableStateFlow<UserAuth.UserAuthData?>(null)
    val authData: StateFlow<UserAuth.UserAuthData?> = _authData.asStateFlow()

    /**
     * Авторизация пользователя через UserAuth интерфейс
     *
     * Используется для провайдеров с логином/паролем (ТНС)
     */
    fun authorizeUser(
        providerId: Long,
        login: String,
        password: String,
        regionId: String?
    ) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchedAccounts.value = null
            _authData.value = null

            try {
                val connector = connectorFactory.getConnector(providerId)

                if (connector is UserAuth) {
                    println("🔐 [AddAccountVM] Авторизация пользователя: $login")

                    val result = connector.userAuth(
                        login = login,
                        password = password,
                        regionId = regionId
                    )

                    result.onSuccess { authData ->
                        if (authData.authSuccess) {
                            _authData.value = authData

                            // Создаём фейковый AccountInfo для отображения в UI
                            _searchedAccounts.value = listOf(
                                GetAccounts.AccountInfo(
                                    accountNumber = login,
                                    address = "Авторизация успешна"
                                )
                            )

                            println("✅ [AddAccountVM] Авторизация успешна")
                            println("   Access token: ${authData.accessToken?.take(20)}...")
                        } else {
                            showError(
                                title = "Ошибка авторизации",
                                message = "Неверный логин или пароль"
                            )
                        }
                    }.onFailure { error ->
                        showError(
                            title = "Ошибка авторизации",
                            message = error.message ?: "Не удалось авторизоваться"
                        )
                        println("❌ [AddAccountVM] Ошибка: ${error.message}")
                    }
                } else {
                    showError(
                        title = "Ошибка",
                        message = "Провайдер не поддерживает авторизацию по логину/паролю"
                    )
                }
            } catch (e: Exception) {
                showError(
                    title = "Ошибка авторизации",
                    message = when (e) {
                        is SocketTimeoutException ->
                            "Сервер не отвечает. Попробуйте позже."
                        else ->
                            "Не удалось выполнить авторизацию: ${e.message}"
                    }
                )
                println("❌ [AddAccountVM] Исключение: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    // =====================================================
    // STATE - поиск адреса
    // =====================================================

    private val _searchedAccounts = MutableStateFlow<List<GetAccounts.AccountInfo>?>(null)
    val searchedAccounts: StateFlow<List<GetAccounts.AccountInfo>?> = _searchedAccounts

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /**
     * Поиск адреса по лицевому счёту
     */
    fun getAccounts(
        providerId: Long,
        accountNumber: String,
        regionId: String?
    ) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchedAccounts.value = null  // ← изменил название

            try {
                val connector = connectorFactory.getConnector(providerId)

                // ✅ Проверяем: поддерживает ли провайдер поиск
                if (connector is GetAccounts) {
                    println("🔍 [AddAccountVM] Поиск аккаунтов...")

                    val result = connector.getAccounts(
                        accountNumber = accountNumber,
                        regionId = regionId
                    )

                    result.onSuccess { accounts ->
                        _searchedAccounts.value = accounts  // ← список AccountInfo
                        println("✅ [AddAccountVM] Найдено аккаунтов: ${accounts.size}")
                        accounts.forEach { account ->
                            println("   Счёт: ${account.accountNumber}, адрес: ${account.address}")
                        }
                    }.onFailure { error ->
                        showError(
                            title = "Аккаунт не найден",
                            message = "Лицевой счёт $accountNumber не найден в системе провайдера"
                        )
                        println("❌ [AddAccountVM] Ошибка поиска: ${error.message}")
                    }
                } else {
                    showError(
                        title = "Ошибка",
                        message = "Провайдер не поддерживает поиск аккаунтов"
                    )
                }
            } catch (e: Exception) {
                showError(
                    title = "Ошибка поиска",
                    message = when (e) {
                        is SocketTimeoutException ->
                            "Сервер не отвечает. Попробуйте позже."
                        else ->
                            "Не удалось выполнить поиск: ${e.message}"
                    }
                )
                println("❌ [AddAccountVM] Исключение: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }


    fun clearSearchResult() {
        _searchedAccounts.value = null
        println("🔄 [AddAccountVM] Результат поиска очищен")
    }

    // =====================================================
    // СОЗДАНИЕ АККАУНТА
    // =====================================================

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createdAccountId = MutableStateFlow<String?>(null)
    val createdAccountId: StateFlow<String?> = _createdAccountId.asStateFlow()

    /**
     * Создать новый аккаунт
     *
     * @param profileId - ID профиля
     * @param accountNumber - номер лицевого счёта
     * @param login - логин (опционально)
     * @param password - пароль (опционально)
     */
    fun createAccount(
        profileId: String,
        accountNumber: String,
        login: String? = null,
        password: String? = null
    ) {
        viewModelScope.launch {
            _isCreating.value = true

            try {
                val providerId = _selectedProviderId.value
                if (providerId == null) {
                    showError("Ошибка", "Провайдер не выбран")
                    return@launch
                }

                // ✅ Получаем regionId из состояния
                val regionId = if (_providerHasRegions.value) {
                    val selectedRegion = _selectedRegionId.value
                    if (selectedRegion == null) {
                        showError("Ошибка", "Выберите регион")
                        return@launch
                    }
                    selectedRegion.toIntOrNull()
                } else {
                    null
                }

                println("💾 [AddAccountVM] createAccount:")
                println("   profileId: $profileId")
                println("   providerId: $providerId")
                println("   accountNumber: $accountNumber")
                println("   regionId: $regionId")  // ✅ ДОЛЖЕН БЫТЬ 15!

                // Создаём аккаунт в БД
                val accountId = accountRepository.addAccount(
                    profileId = profileId,
                    providerId = providerId,
                    accountNumber = accountNumber,
                    regionId = regionId,  // ✅ ПЕРЕДАЁМ!
                    login = login,
                    password = password
                )

                _createdAccountId.value = accountId
                println("✅ [AddAccountVM] Аккаунт создан: $accountId")

            } catch (e: IllegalArgumentException) {
                showError(
                    title = "Аккаунт уже добавлен",
                    message = "Лицевой счёт $accountNumber уже существует в этом профиле"
                )
            } catch (e: Exception) {
                showError(
                    title = "Ошибка сохранения",
                    message = "Не удалось сохранить аккаунт: ${e.message}"
                )
                e.printStackTrace()
            } finally {
                _isCreating.value = false
            }
        }
    }


    /**
     * Сбросить состояние после создания
     */
    fun resetCreation() {
        _createdAccountId.value = null
        clearSelection()
        clearSearchResult()
        println("🔄 [AddAccountVM] Создание сброшено")
    }
}
