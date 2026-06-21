package com.personx.hermatic.data.repository

import com.personx.hermatic.data.api.HermesApi
import com.personx.hermatic.data.db.ChatDao
import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.model.toEntity
import com.personx.hermatic.data.model.toMessage
import com.personx.hermatic.data.model.ChatChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class HermesRepository(
    private val api: HermesApi,
    private val chatDao: ChatDao,
    private val json: Json,
) {
    fun getChatHistory(): Flow<List<Message>> {
        return chatDao.getAllMessages().map { entities ->
            entities.map { it.toMessage() }
        }
    }

    fun chatStream(messages: List<Message>): Flow<String> = flow {
        // Save user message to DB
        val lastMessage = messages.last()
        chatDao.insertMessage(lastMessage.toEntity())

        val request = ChatRequest(messages = messages, stream = true)
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

    suspend fun chat(messages: List<Message>): String {
        // Save user message to DB
        val lastMessage = messages.last()
        chatDao.insertMessage(lastMessage.toEntity())

        val request = ChatRequest(messages = messages)
        val response = api.chatCompletions(request)
        val content = response.choices.firstOrNull()?.message?.content ?: "No response"

        // Save bot message to DB
        chatDao.insertMessage(Message(role = "assistant", content = content).toEntity())

        return content
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun performSelfDestruct(retentionPeriodMs: Long) {
        val threshold = System.currentTimeMillis() - retentionPeriodMs
        chatDao.deleteMessagesOlderThan(threshold)
    }
}
