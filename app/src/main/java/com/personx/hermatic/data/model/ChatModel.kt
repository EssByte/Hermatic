package com.personx.hermatic.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String = "hermes-agent",
    val messages: List<Message>,
    val stream: Boolean = false
)

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>
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
