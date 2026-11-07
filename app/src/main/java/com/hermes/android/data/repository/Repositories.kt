package com.hermes.android.data.repository

import com.hermes.android.data.db.AppDatabase
import com.hermes.android.data.db.MessageEntity
import com.hermes.android.data.db.SessionEntity
import com.hermes.android.data.model.ChatMessage
import com.hermes.android.data.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(private val database: AppDatabase) {
    fun getAllSessions(): Flow<List<Session>> = database.sessionDao().getAllSessions()
        .map { it.map { it.toDomain() } }

    suspend fun getSessionById(id: String): Session? = database.sessionDao().getSessionById(id)?.toDomain()

    suspend fun createSession(session: Session) {
        database.sessionDao().insert(SessionEntity.fromDomain(session))
    }

    suspend fun updateSession(session: Session) {
        database.sessionDao().update(SessionEntity.fromDomain(session))
    }

    suspend fun deleteSession(id: String) {
        database.sessionDao().deleteById(id)
        database.messageDao().deleteBySessionId(id)
    }

    suspend fun incrementMessageCount(sessionId: String) {
        database.sessionDao().getSessionById(sessionId)?.let { entity ->
            val updated = entity.copy(messageCount = entity.messageCount + 1, updatedAt = kotlinx.datetime.Clock.System.now())
            database.sessionDao().update(updated)
        }
    }
}

class MessageRepository(private val database: AppDatabase) {
    fun getMessagesBySession(sessionId: String): Flow<List<ChatMessage>> = database.messageDao().getMessagesBySession(sessionId)
        .map { it.map { it.toDomain() } }

    suspend fun getMessagesBySessionOnce(sessionId: String): List<ChatMessage> =
        database.messageDao().getMessagesBySessionOnce(sessionId).map { it.toDomain() }

    suspend fun insertMessage(message: ChatMessage) {
        database.messageDao().insert(MessageEntity.fromDomain(message))
    }

    suspend fun insertMessages(messages: List<ChatMessage>) {
        database.messageDao().insertAll(messages.map { MessageEntity.fromDomain(it) })
    }

    suspend fun updateMessage(message: ChatMessage) {
        database.messageDao().update(MessageEntity.fromDomain(message))
    }

    suspend fun updateMessages(messages: List<ChatMessage>) {
        database.messageDao().updateAll(messages.map { MessageEntity.fromDomain(it) })
    }

    suspend fun deleteMessage(id: String) {
        database.messageDao().deleteById(id)
    }
}