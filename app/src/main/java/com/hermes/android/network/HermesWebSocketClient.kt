package com.hermes.android.network

import com.hermes.android.data.model.JsonRpcMessage
import com.hermes.android.data.model.ServerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlinx.serialization.encodeToString

class HermesWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val httpClient: HermesHttpClient,
    private val settingsStore: com.hermes.android.data.preferences.SettingsDataStore
) {
    private var webSocket: WebSocket? = null
    private val messageChannel = Channel<ServerEvent>(Channel.UNLIMITED)
    private val connectionState = MutableStateFlow(ConnectionState.Disconnected)
    private var currentTicket: String? = null
    private var baseUrl: String = ""
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 2000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val messages: ReceiveChannel<ServerEvent> = messageChannel
    val connectionStateFlow: StateFlow<ConnectionState> = connectionState.asStateFlow()

    enum class ConnectionState {
        Disconnected, Connecting, Connected, Reconnecting, Error
    }

    /**
     * Connects to the Hermes gateway on the local network.
     * [baseUrl] is the plain-HTTP LAN base URL, e.g. http://192.168.1.100:9119
     * producing ws://192.168.1.100:9119/api/ws?ticket=...
     */
    fun connect(baseUrl: String, ticket: String) {
        this.baseUrl = baseUrl
        this.currentTicket = ticket
        connectionState.value = ConnectionState.Connecting

        val wsUrl = baseUrl
            .trimEnd('/')
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/api/ws?ticket=$ticket"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndEmit(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                parseAndEmit(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectionState.value = ConnectionState.Error
                attemptReconnect()
            }
        })
    }

    private fun parseAndEmit(text: String) {
        try {
            val method = json.parseToJsonElement(text).jsonObject["method"]?.jsonPrimitive?.content
                ?: return
            val event: ServerEvent? = when (method) {
                "message.delta" -> json.decodeFromString<ServerEvent.MessageDelta>(text)
                "tool.start" -> json.decodeFromString<ServerEvent.ToolStart>(text)
                "tool.progress" -> json.decodeFromString<ServerEvent.ToolProgress>(text)
                "tool.complete" -> json.decodeFromString<ServerEvent.ToolComplete>(text)
                "tool.error" -> json.decodeFromString<ServerEvent.ToolError>(text)
                "gateway.ready" -> json.decodeFromString<ServerEvent.GatewayReady>(text)
                "approval.request" -> json.decodeFromString<ServerEvent.ApprovalRequest>(text)
                "error" -> json.decodeFromString<ServerEvent.Error>(text)
                else -> null
            }
            event?.let { messageChannel.trySend(it) }
        } catch (e: Exception) {
            // Ignore parse errors for unknown message types
        }
    }

    private fun attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            connectionState.value = ConnectionState.Error
            return
        }
        reconnectAttempts++
        connectionState.value = ConnectionState.Reconnecting
        scope.launch {
            delay(reconnectDelayMs * reconnectAttempts)
            // A ws ticket is single-use (30 s TTL): refresh credentials and mint a new one.
            try {
                val settings = settingsStore.settingsFlow.first()
                val host = settings.localIp.trim()
                    .removePrefix("http://").removePrefix("https://").trimEnd('/')
                if (host.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
                    connectionState.value = ConnectionState.Error
                    return@launch
                }
                val url = "http://$host:9119"
                httpClient.login(url, settings.username, settings.password)
                    .onSuccess { loginResponse ->
                        val cookies = loginResponse.cookies
                        if (cookies.isNullOrEmpty()) {
                            connectionState.value = ConnectionState.Error
                            return@onSuccess
                        }
                        httpClient.getWsTicket(url, cookies)
                            .onSuccess { connect(url, it.ticket) }
                            .onFailure { connectionState.value = ConnectionState.Error }
                    }
                    .onFailure { connectionState.value = ConnectionState.Error }
            } catch (e: Exception) {
                connectionState.value = ConnectionState.Error
            }
        }
    }

    fun send(jsonRpcMessage: JsonRpcMessage) {
        val text = json.encodeToString(jsonRpcMessage)
        webSocket?.send(text)
    }

    fun sendRequest(id: String, method: String, params: Map<String, String>? = null) {
        val paramsObject: JsonObject? = params?.let { values ->
            buildJsonObject {
                values.forEach { (key, value) -> put(key, value) }
            }
        }
        send(JsonRpcMessage.Request(id = id, method = method, params = paramsObject))
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        currentTicket = null
        connectionState.value = ConnectionState.Disconnected
    }

    fun isConnected(): Boolean = connectionState.value == ConnectionState.Connected
}
