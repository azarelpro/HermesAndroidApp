package com.hermes.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.hermes.android.data.model.MessageRole
import com.hermes.android.data.model.Session
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Entity(tableName = "sessions")
@Serializable
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messageCount: Int = 0
) {
    fun toDomain(): Session = Session(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount
    )

    companion object {
        fun fromDomain(session: Session): SessionEntity = SessionEntity(
            id = session.id,
            title = session.title,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            messageCount = session.messageCount
        )
    }
}

@Entity(tableName = "messages")
@Serializable
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
) {
    fun toDomain(): com.hermes.android.data.model.ChatMessage = com.hermes.android.data.model.ChatMessage(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp,
        toolCallId = toolCallId,
        toolName = toolName,
        isStreaming = isStreaming,
        isError = isError
    )

    companion object {
        fun fromDomain(message: com.hermes.android.data.model.ChatMessage): MessageEntity = MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp,
            toolCallId = message.toolCallId,
            toolName = message.toolName,
            isStreaming = message.isStreaming,
            isError = message.isError
        )
    }
}