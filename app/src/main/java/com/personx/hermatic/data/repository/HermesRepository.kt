package com.personx.hermatic.data.repository

import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.api.HermesApi
import com.personx.hermatic.data.db.ChatDao
import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.model.toEntity
import com.personx.hermatic.data.model.toMessage
import com.personx.hermatic.data.model.ChatChunk
import com.personx.hermatic.data.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class HermesRepository(
    private val apiClient: ApiClient,
    private val chatDao: ChatDao,
    private val json: Json,
) {
    private val api: HermesApi get() = apiClient.hermesApi

    fun getChatHistory(): Flow<List<Message>> {
        return chatDao.getAllMessages().map { entities ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun getModels(): List<ModelInfo> {
        return try {
            api.getModels().data
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun chatStream(
        messages: List<Message>,
        model: String,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): Flow<String> = flow {
        // Prepare messages with system prompt
        val fullMessages = mutableListOf(Message(role = "system", content = systemPrompt))
        fullMessages.addAll(messages)

        // Save user message to DB (last one in the input list)
        val userMessage = messages.last()
        chatDao.insertMessage(userMessage.toEntity())

        val request = ChatRequest(
            model = model,
            messages = fullMessages,
            stream = true,
            temperature = temperature,
            max_tokens = maxTokens
        )
        val responseBody = api.chatCompletionsStream(request)
        
        val fullContent = StringBuilder()
        
        responseBody.byteStream().bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("data: ")) {
                    val data = line.substring(6)
                    if (data != "[DONE]") {
                        try {
                            val chunk = json.decodeFromString<ChatChunk>(data)
                            chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                                fullContent.append(content)
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // Skip invalid chunks
                        }
                    }
                }
            }
        }

        // Save full bot message to DB
        if (fullContent.isNotEmpty()) {
            chatDao.insertMessage(Message(role = "assistant", content = fullContent.toString()).toEntity())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun checkHealth(): Boolean {
        return try {
            api.checkHealth()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun performSelfDestruct(retentionPeriodMs: Long) {
        val threshold = System.currentTimeMillis() - retentionPeriodMs
        chatDao.deleteMessagesOlderThan(threshold)
    }
}
