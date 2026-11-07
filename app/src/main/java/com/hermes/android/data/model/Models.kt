package com.hermes.android.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonObject? = null
)

@Serializable
data class JsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonObject? = null
)

@Serializable
sealed interface JsonRpcMessage {
    @Serializable
    data class Request(
        val jsonrpc: String = "2.0",
        val id: String,
        val method: String,
        val params: JsonObject? = null
    ) : JsonRpcMessage

    @Serializable
    data class Response(
        val jsonrpc: String = "2.0",
        val id: String,
        val result: JsonObject? = null,
        val error: JsonRpcError? = null
    ) : JsonRpcMessage

    @Serializable
    data class Notification(
        val jsonrpc: String = "2.0",
        val method: String,
        val params: JsonObject? = null
    ) : JsonRpcMessage
}

@Serializable
data class AuthLoginRequest(
    val provider: String = "basic",
    val username: String,
    val password: String
)

@Serializable
data class AuthLoginResponse(
    val ok: Boolean? = null,
    val success: Boolean = false,
    val next: String? = null,
    val cookies: Map<String, String>? = null
)

@Serializable
data class WsTicketRequest(
    @SerialName("cookies") val cookies: Map<String, String>? = null
)

@Serializable
data class WsTicketResponse(
    val ticket: String,
    @SerialName("ttl_seconds") val ttlSeconds: Int = 30,
    @SerialName("expires_in") val expiresIn: Int = 30
)

@Serializable
data class SessionCreateRequest(
    val title: String? = null
)

@Serializable
data class SessionCreateResponse(
    val session_id: String,
    val title: String
)

@Serializable
data class PromptSubmitRequest(
    val session_id: String,
    val prompt: String,
    val files: List<String>? = null
)

@Serializable
data class PromptSubmitResponse(
    val message_id: String
)

@Serializable
data class MessageDeltaEvent(
    val session_id: String,
    val message_id: String,
    val delta: String,
    val done: Boolean
)

@Serializable
data class ToolStartEvent(
    val session_id: String,
    val message_id: String,
    val tool_call_id: String,
    val name: String,
    val arguments: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class ToolProgressEvent(
    val session_id: String,
    val message_id: String,
    val tool_call_id: String,
    val progress: String
)

@Serializable
data class ToolCompleteEvent(
    val session_id: String,
    val message_id: String,
    val tool_call_id: String,
    val result: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class ToolErrorEvent(
    val session_id: String,
    val message_id: String,
    val tool_call_id: String,
    val error: String
)

@Serializable
data class GatewayReadyEvent(
    val session_id: String
)

@Serializable
sealed interface ServerEvent {
    @Serializable
    data class MessageDelta(
        val session_id: String = "",
        val message_id: String = "",
        val delta: String = "",
        val done: Boolean = false
    ) : ServerEvent

    @Serializable
    data class ToolStart(
        val session_id: String = "",
        val message_id: String = "",
        val tool_call_id: String = "",
        val name: String = "",
        val arguments: JsonObject = JsonObject(emptyMap())
    ) : ServerEvent

    @Serializable
    data class ToolProgress(
        val session_id: String = "",
        val message_id: String = "",
        val tool_call_id: String = "",
        val progress: String = ""
    ) : ServerEvent

    @Serializable
    data class ToolComplete(
        val session_id: String = "",
        val message_id: String = "",
        val tool_call_id: String = "",
        val result: JsonObject = JsonObject(emptyMap())
    ) : ServerEvent

    @Serializable
    data class ToolError(
        val session_id: String = "",
        val message_id: String = "",
        val tool_call_id: String = "",
        val error: String = ""
    ) : ServerEvent

    @Serializable
    data class GatewayReady(
        val session_id: String = ""
    ) : ServerEvent

    @Serializable
    data class ApprovalRequest(
        val session_id: String = "",
        val message_id: String = "",
        val request_id: String = "",
        val title: String = "",
        val description: String = "",
        val options: List<String> = emptyList()
    ) : ServerEvent

    @Serializable
    data class Error(
        val session_id: String = "",
        val message_id: String? = null,
        val code: String = "",
        val message: String = ""
    ) : ServerEvent
}

@Serializable
data class Session(
    val id: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messageCount: Int = 0
)

@Serializable
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

enum class MessageRole {
    USER, ASSISTANT, TOOL, SYSTEM, ERROR
}

@Serializable
data class AppSettings(
    val localIp: String = "192.168.100.4",
    val username: String = "asarel",
    val password: String = "asarel123",
    val autoConnect: Boolean = true
)
