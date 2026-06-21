package com.personx.hermatic.data.db

import androidx.room.TypeConverter
import com.personx.hermatic.data.model.ToolCall
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromToolCallList(value: List<ToolCall>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toToolCallList(value: String?): List<ToolCall>? {
        return value?.let { json.decodeFromString(it) }
    }
}
