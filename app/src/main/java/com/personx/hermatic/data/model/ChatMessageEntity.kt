package com.personx.hermatic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default",
    val role: String,
    val content: String,
    val tool_calls: List<ToolCall>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val transcription: String? = null
)

fun ChatMessageEntity.toMessage() = Message(
    role = role, 
    content = content, 
    tool_calls = tool_calls,
    timestamp = timestamp,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    transcription = transcription,
    dbId = id
)

fun Message.toEntity(sessionId: String) = ChatMessageEntity(
    sessionId = sessionId,
    role = role, 
    content = content, 
    tool_calls = tool_calls,
    timestamp = timestamp,
    imageUrl = imageUrl,
    audioUrl = audioUrl,
    transcription = transcription
)
