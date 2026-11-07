package com.hermes.android.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.R
import com.hermes.android.data.model.ChatMessage
import com.hermes.android.data.model.MessageRole
import com.hermes.android.network.HermesWebSocketClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onBackClick: () -> Unit,
    wsClient: HermesWebSocketClient,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    LaunchedEffect(sessionId) {
        viewModel.openSession(sessionId)
    }

    val success = uiState as? ChatUiState.Success
    val failure = uiState as? ChatUiState.Error

    when {
        failure != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error: ${failure.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.createSession() }) {
                        Text("Create New Session")
                    }
                }
            }
        }
        success != null -> {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text("Session ${sessionId.take(8)}") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = if (success.isConnected) Color.Green else Color.Red,
                                    radius = 6.dp.toPx()
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = listState
                ) {
                    items(success.messages) { message ->
                        MessageBubble(
                            message = message,
                            formatter = formatter,
                            isCurrentUser = message.role == MessageRole.USER
                        )
                    }
                }

                ChatInputArea(
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendPrompt(messageText)
                            messageText = ""
                        }
                    },
                    isLoading = success.isStreaming,
                    isConnected = success.isConnected
                )
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    formatter: SimpleDateFormat,
    isCurrentUser: Boolean
) {
    val bubbleColor = when (message.role) {
        MessageRole.USER -> MaterialTheme.colorScheme.primary
        MessageRole.ASSISTANT -> MaterialTheme.colorScheme.surfaceContainerHigh
        MessageRole.TOOL -> MaterialTheme.colorScheme.secondaryContainer
        MessageRole.ERROR -> MaterialTheme.colorScheme.errorContainer
        MessageRole.SYSTEM -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val textColor = when (message.role) {
        MessageRole.USER -> MaterialTheme.colorScheme.onPrimary
        MessageRole.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        MessageRole.SYSTEM -> MaterialTheme.colorScheme.onTertiaryContainer
        MessageRole.TOOL -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(color = bubbleColor, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            val label = when (message.role) {
                MessageRole.USER -> "You"
                MessageRole.ASSISTANT -> "Hermes"
                MessageRole.TOOL -> message.toolName ?: stringResource(R.string.tool_call)
                MessageRole.ERROR -> "Error"
                MessageRole.SYSTEM -> "System"
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message.content, fontSize = 15.sp, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatter.format(Date(message.timestamp.toEpochMilliseconds())),
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ChatInputArea(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.chat_placeholder)) },
            maxLines = 4,
            enabled = !isLoading && isConnected
        )
        IconButton(
            onClick = onSendClick,
            enabled = messageText.isNotBlank() && isConnected && !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = stringResource(R.string.send_button),
                tint = if (messageText.isNotBlank() && isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
