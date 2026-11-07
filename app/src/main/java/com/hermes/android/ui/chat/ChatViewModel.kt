package com.hermes.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.model.*
import com.hermes.android.data.repository.MessageRepository
import com.hermes.android.data.repository.SessionRepository
import com.hermes.android.network.HermesWebSocketClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val messages: List<ChatMessage>,
        val sessionId: String,
        val isConnected: Boolean,
        val isStreaming: Boolean
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val wsClient: HermesWebSocketClient
) : ViewModel() {

    private var currentSessionId: String? = null
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState

    private val pendingToolCalls = mutableMapOf<String, ChatMessage>()

    fun openSession(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            messageRepository.getMessagesBySession(sessionId)
                .combine(wsClient.connectionStateFlow) { messages, connState ->
                    val isStreaming = messages.any { it.isStreaming }
                    ChatUiState.Success(
                        messages = messages,
                        sessionId = sessionId,
                        isConnected = connState == HermesWebSocketClient.ConnectionState.Connected,
                        isStreaming = isStreaming
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
        listenToWebSocket()
    }

    private fun listenToWebSocket() {
        viewModelScope.launch {
            for (event in wsClient.messages) {
                when (event) {
                    is ServerEvent.MessageDelta -> handleMessageDelta(event)
                    is ServerEvent.ToolStart -> handleToolStart(event)
                    is ServerEvent.ToolProgress -> handleToolProgress(event)
                    is ServerEvent.ToolComplete -> handleToolComplete(event)
                    is ServerEvent.ToolError -> handleToolError(event)
                    is ServerEvent.GatewayReady -> handleGatewayReady(event)
                    is ServerEvent.Error -> handleServerError(event)
                    is ServerEvent.ApprovalRequest -> handleApprovalRequest(event)
                }
            }
        }
    }

    private fun handleMessageDelta(event: ServerEvent.MessageDelta) {
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    val existingMessage = messageRepository.getMessagesBySessionOnce(sessionId)
                        .findLast { it.id == event.message_id }
                    
                    if (existingMessage != null) {
                        val updated = existingMessage.copy(
                            content = existingMessage.content + event.delta,
                            isStreaming = !event.done
                        )
                        messageRepository.updateMessage(updated)
                    } else {
                        val newMessage = ChatMessage(
                            id = event.message_id,
                            sessionId = sessionId,
                            role = MessageRole.ASSISTANT,
                            content = event.delta,
                            timestamp = kotlinx.datetime.Clock.System.now(),
                            isStreaming = !event.done
                        )
                        messageRepository.insertMessage(newMessage)
                    }
                    
                    if (event.done) {
                        sessionRepository.incrementMessageCount(sessionId)
                    }
                }
            }
        }
    }

    private fun handleToolStart(event: ServerEvent.ToolStart) {
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    val toolMessage = ChatMessage(
                        id = event.tool_call_id,
                        sessionId = sessionId,
                        role = MessageRole.TOOL,
                        content = "🔧 Calling ${event.name}…",
                        timestamp = kotlinx.datetime.Clock.System.now(),
                        toolCallId = event.tool_call_id,
                        toolName = event.name,
                        isStreaming = true
                    )
                    messageRepository.insertMessage(toolMessage)
                    pendingToolCalls[event.tool_call_id] = toolMessage
                }
            }
        }
    }

    private fun handleToolProgress(event: ServerEvent.ToolProgress) {
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    pendingToolCalls[event.tool_call_id]?.let { toolMessage ->
                        val updated = toolMessage.copy(content = "🔧 ${event.progress}")
                        messageRepository.updateMessage(updated)
                        pendingToolCalls[event.tool_call_id] = updated
                    }
                }
            }
        }
    }

    private fun handleToolComplete(event: ServerEvent.ToolComplete) {
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    pendingToolCalls.remove(event.tool_call_id)?.let { toolMessage ->
                        val resultJson = kotlinx.serialization.json.Json { prettyPrint = true }.encodeToString(event.result)
                        val updated = toolMessage.copy(
                            content = "✅ ${toolMessage.toolName} completed\n$resultJson",
                            isStreaming = false
                        )
                        messageRepository.updateMessage(updated)
                    }
                }
            }
        }
    }

    private fun handleToolError(event: ServerEvent.ToolError) {
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    pendingToolCalls.remove(event.tool_call_id)?.let { toolMessage ->
                        val updated = toolMessage.copy(
                            content = "❌ ${toolMessage.toolName} failed: ${event.error}",
                            isStreaming = false,
                            isError = true
                        )
                        messageRepository.updateMessage(updated)
                    }
                }
            }
        }
    }

    private fun handleGatewayReady(event: ServerEvent.GatewayReady) {
        // Session is ready, nothing special needed
    }

    private fun handleServerError(event: ServerEvent.Error) {
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = MessageRole.ERROR,
                        content = "${event.code}: ${event.message}",
                        timestamp = kotlinx.datetime.Clock.System.now(),
                        isError = true
                    )
                    messageRepository.insertMessage(errorMessage)
                }
            }
        }
    }

    private fun handleApprovalRequest(event: ServerEvent.ApprovalRequest) {
        // For now, just show as a message - could add approval UI later
        currentSessionId?.let { sessionId ->
            if (event.session_id == sessionId) {
                viewModelScope.launch {
                    val approvalMessage = ChatMessage(
                        id = event.request_id,
                        sessionId = sessionId,
                        role = MessageRole.SYSTEM,
                        content = "⚠️ Approval required: ${event.title}\n${event.description}\nOptions: ${event.options.joinToString(", ")}",
                        timestamp = kotlinx.datetime.Clock.System.now()
                    )
                    messageRepository.insertMessage(approvalMessage)
                }
            }
        }
    }

    fun sendPrompt(prompt: String) {
        currentSessionId?.let { sessionId ->
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = MessageRole.USER,
                content = prompt,
                timestamp = kotlinx.datetime.Clock.System.now()
            )
            
            viewModelScope.launch {
                messageRepository.insertMessage(userMessage)
                sessionRepository.incrementMessageCount(sessionId)
                
                val messageId = UUID.randomUUID().toString()
                wsClient.sendRequest(
                    id = messageId,
                    method = "prompt.submit",
                    params = mapOf(
                        "session_id" to sessionId,
                        "prompt" to prompt,
                        "message_id" to messageId
                    )
                )
            }
        }
    }

    fun createSession() {
        viewModelScope.launch {
            val session = Session(
                id = UUID.randomUUID().toString(),
                title = "New Chat",
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            sessionRepository.createSession(session)
            openSession(session.id)
        }
    }
}