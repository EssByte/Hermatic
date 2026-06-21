package com.personx.hermatic.data.repository

import com.personx.hermatic.data.api.HermesApi
import com.personx.hermatic.data.db.ChatDao
import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.Message
import com.personx.hermatic.data.model.toEntity
import com.personx.hermatic.data.model.toMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HermesRepository(
    private val api: HermesApi,
    private val chatDao: ChatDao
) {
    fun getChatHistory(): Flow<List<Message>> {
        return chatDao.getAllMessages().map { entities ->
            entities.map { it.toMessage() }
        }
    }

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
}
