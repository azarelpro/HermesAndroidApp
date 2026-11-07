package com.hermes.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.model.AppSettings
import com.hermes.android.data.preferences.SettingsDataStore
import com.hermes.android.network.HermesWebSocketClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data class Current(val settings: AppSettings, val isConnected: Boolean) : SettingsUiState
    data object Saved : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

class SettingsViewModel(
    private val settingsStore: SettingsDataStore,
    private val wsClient: HermesWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Current(AppSettings(), false))
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsStore.settingsFlow
                .combine(wsClient.connectionStateFlow) { settings, connState ->
                    SettingsUiState.Current(settings, connState == HermesWebSocketClient.ConnectionState.Connected)
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            val success = settingsStore.saveSettings(settings)
            if (success) {
                _uiState.value = SettingsUiState.Saved
                delay(1000)
                val currentSettings = settingsStore.settingsFlow.first()
                _uiState.value = SettingsUiState.Current(currentSettings, wsClient.isConnected())
            } else {
                _uiState.value = SettingsUiState.Error("Failed to save settings")
            }
        }
    }

    fun disconnect() {
        wsClient.disconnect()
    }

    fun reconnect(settings: AppSettings) {
        if (settings.localIp.isNotBlank() && settings.username.isNotBlank() && settings.password.isNotBlank()) {
            // The actual reconnection is handled by LoginViewModel
            // This just triggers the flow
        }
    }
}
