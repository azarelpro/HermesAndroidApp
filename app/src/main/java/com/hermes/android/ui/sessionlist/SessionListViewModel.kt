package com.hermes.android.ui.sessionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.model.Session
import com.hermes.android.data.repository.SessionRepository
import com.hermes.android.network.HermesWebSocketClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface SessionListUiState {
    data object Loading : SessionListUiState
    data class Success(val sessions: List<Session>, val isConnected: Boolean) : SessionListUiState
    data class Error(val message: String) : SessionListUiState
}

class SessionListViewModel(
    private val sessionRepository: SessionRepository,
    private val wsClient: HermesWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionListUiState>(SessionListUiState.Loading)
    val uiState: StateFlow<SessionListUiState> = _uiState

    private val connectionState = wsClient.connectionStateFlow

    init {
        viewModelScope.launch {
            sessionRepository.getAllSessions()
                .combine(connectionState) { sessions, connState ->
                    SessionListUiState.Success(sessions, connState == HermesWebSocketClient.ConnectionState.Connected)
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun createNewSession(title: String = "New Chat") {
        viewModelScope.launch {
            val session = Session(
                id = UUID.randomUUID().toString(),
                title = title,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            sessionRepository.createSession(session)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun updateSessionTitle(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            sessionRepository.getSessionById(sessionId)?.let { session ->
                val updated = session.copy(title = newTitle, updatedAt = kotlinx.datetime.Clock.System.now())
                sessionRepository.updateSession(updated)
            }
        }
    }
}