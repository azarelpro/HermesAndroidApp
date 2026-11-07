package com.hermes.android.data.db

import androidx.room.TypeConverter
import com.hermes.android.data.model.MessageRole
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilliseconds()
    }

    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun fromMessageRole(value: MessageRole?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMessageRole(value: String?): MessageRole? {
        return value?.let { MessageRole.valueOf(it) }
    }
}