package com.personx.hermatic.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Message(
    val role: String,
    val content: String,
    @Transient val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ChatRequest(
    val model: String = "hermes-agent",
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    val max_tokens: Int? = null
)

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val model: String? = null
)

@Serializable
data class ModelListResponse(
    val data: List<ModelInfo>
)

@Serializable
data class ModelInfo(
    val id: String,
    val owned_by: String? = null
)

@Serializable
data class Choice(
    val message: Message? = null,
    val delta: Delta? = null,
    val finish_reason: String? = null
)

@Serializable
data class Delta(
    val content: String? = null
)

@Serializable
data class ChatChunk(
    val id: String,
    val choices: List<Choice>
)
