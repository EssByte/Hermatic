package com.personx.hermatic.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

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
data class SkillListResponse(
    val data: List<SkillInfo>
)

@Serializable
data class SkillInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null
)

@Serializable
data class ToolsetListResponse(
    val data: List<ToolsetInfo>
)

@Serializable
data class ToolsetInfo(
    val id: String,
    val name: String,
    val tools: List<String> = emptyList()
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
