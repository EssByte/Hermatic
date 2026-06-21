package com.personx.hermatic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun ChatMessageEntity.toMessage() = Message(role = role, content = content, timestamp = timestamp)
fun Message.toEntity() = ChatMessageEntity(role = role, content = content, timestamp = timestamp)
