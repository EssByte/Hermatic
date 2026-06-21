package com.personx.hermatic.data.repository

import com.personx.hermatic.data.api.HermesApi
import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.Message

class HermesRepository(private val api: HermesApi) {
    suspend fun chat(messages: List<Message>): String {
        val request = ChatRequest(messages = messages)
        val response = api.chatCompletions(request)
        return response.choices.firstOrNull()?.message?.content ?: "No response"
    }
}
