package com.hermes.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.model.AppSettings
import com.hermes.android.data.preferences.SettingsDataStore
import com.hermes.android.network.HermesHttpClient
import com.hermes.android.network.HermesWebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data class Idle(val savedSettings: AppSettings) : LoginUiState
    data class Loading(val message: String) : LoginUiState
    data class Success(val wsClient: HermesWebSocketClient) : LoginUiState
    data class Error(val message: String, val savedSettings: AppSettings) : LoginUiState
}

class LoginViewModel(
    private val httpClient: HermesHttpClient,
    private val wsClient: HermesWebSocketClient,
    private val settingsStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle(AppSettings()))
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        loadSavedSettings()
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            val settings = settingsStore.settingsFlow.first()
            _uiState.value = LoginUiState.Idle(settings)
            // Baked-in credentials: connect straight away, no typing required.
            if (settings.autoConnect &&
                settings.localIp.isNotBlank() &&
                settings.username.isNotBlank() &&
                settings.password.isNotBlank()
            ) {
                login(settings.localIp, settings.username, settings.password)
            }
        }
    }

    /** Hermes dashboard is reached directly on the LAN over plain HTTP. */
    private fun buildBaseUrl(localIp: String): String {
        val host = localIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        return "http://$host:9119"
    }

    fun login(localIp: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading("Connecting to Hermes…")

            val baseUrl = buildBaseUrl(localIp)
            val loginResult = httpClient.login(baseUrl, username, password)

            loginResult.onSuccess { loginResponse ->
                val cookies = loginResponse.cookies
                if (cookies.isNullOrEmpty()) {
                    _uiState.value = LoginUiState.Error(
                        "Login succeeded but no cookies received",
                        AppSettings(localIp = localIp, username = username, password = password)
                    )
                    return@onSuccess
                }
                val ticketResult = httpClient.getWsTicket(baseUrl, cookies)
                ticketResult.onSuccess { ticketResponse ->
                    wsClient.connect(baseUrl, ticketResponse.ticket)
                    _uiState.value = LoginUiState.Success(wsClient)
                    settingsStore.saveSettings(
                        AppSettings(
                            localIp = localIp,
                            username = username,
                            password = password,
                            autoConnect = true
                        )
                    )
                }.onFailure { e ->
                    _uiState.value = LoginUiState.Error(
                        "Failed to get WebSocket ticket: ${e.message}",
                        AppSettings(localIp = localIp, username = username, password = password)
                    )
                }
            }.onFailure { e ->
                _uiState.value = LoginUiState.Error(
                    "Login failed: ${e.message}",
                    AppSettings(localIp = localIp, username = username, password = password)
                )
            }
        }
    }

    fun retry(savedSettings: AppSettings) {
        _uiState.value = LoginUiState.Idle(savedSettings)
    }
}
