package com.personx.hermatic.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

@Serializable
data class Message(
    val role: String,
    val content: String, // Kept as String for DB and simple use
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null,
    val name: String? = null,
    @Transient val timestamp: Long = System.currentTimeMillis(),
    @Transient val imageUrl: String? = null, // Local or base64
    @Transient val audioUrl: String? = null,
    @Transient val transcription: String? = null,
    @Transient val toolResults: Map<String, String>? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

@Serializable
data class ChatRequest(
    val model: String = "hermes-agent",
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    val max_tokens: Int? = null,
    val tools: List<ToolDefinition>? = null,
    val tool_choice: String? = null
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null
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
    val content: String? = null,
    val tool_calls: List<ToolCallChunk>? = null
)

@Serializable
data class ToolCallChunk(
    val index: Int,
    val id: String? = null,
    val type: String? = null,
    val function: FunctionCallChunk? = null
)

@Serializable
data class FunctionCallChunk(
    val name: String? = null,
    val arguments: String? = null
)

@Serializable
data class ChatChunk(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class RunResponse(
    val id: String,
    val status: String,
    val model: String
)

@Serializable
data class RunEvent(
    val type: String,
    val data: JsonElement? = null
)

@Serializable
data class Session(
    val id: String,
    val name: String? = null,
    val created_at: Long = System.currentTimeMillis()
)

@Serializable
data class SessionListResponse(
    val data: List<Session>
)

@Serializable
data class HealthDetailed(
    val status: String,
    val active_sessions: Int? = null,
    val running_agents: Int? = null,
    val resource_usage: JsonElement? = null
)

@Serializable
data class JobCreateRequest(
    val prompt: String,
    val schedule: String = "*/30 * * * *",
    val skills: List<String>? = null,
    val provider: String? = null,
    val delivery: JsonElement? = null
)

@Serializable
data class StopRunResponse(
    val status: String
)

@Serializable
data class SessionChatRequest(
    val input: String,
    val instructions: String? = null,
    val conversation_history: List<Message>? = null
)

enum class ToolCallStatus { Running, Completed, Failed }

data class DisplayToolCall(
    val callId: String,
    val name: String,
    val arguments: String,
    val result: String? = null,
    val status: ToolCallStatus = ToolCallStatus.Running
)

sealed class ChatStreamEvent {
    data class TextDelta(val content: String) : ChatStreamEvent()
    data class ToolStarted(val name: String, val arguments: String, val callId: String) : ChatStreamEvent()
    data class ToolCompleted(val callId: String, val output: String) : ChatStreamEvent()
    data object RunCompleted : ChatStreamEvent()
}

@Serializable
data class ChatStreamEventData(
    val content: String? = null,
    val name: String? = null,
    val arguments: String? = null,
    @SerialName("call_id") val call_id: String? = null,
    val output: String? = null
)
