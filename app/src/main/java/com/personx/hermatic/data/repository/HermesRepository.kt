package com.personx.hermatic.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.personx.hermatic.data.api.ApiClient
import com.personx.hermatic.data.api.HermesApi
import com.personx.hermatic.data.db.ChatDao
import com.personx.hermatic.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream

class HermesRepository(
    private val apiClient: ApiClient,
    private val chatDao: ChatDao,
    private val json: Json,
) {
    private val api: HermesApi get() = apiClient.hermesApi

    fun getChatHistory(sessionId: String): Flow<List<Message>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toMessage() }
        }
    }
    
    fun getSessions(): Flow<List<String>> {
        return chatDao.getAllSessionIds()
    }

    suspend fun getModels(): List<ModelInfo> {
        return try {
            api.getModels().data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSkills(): List<SkillInfo> {
        return try {
            api.getSkills().data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getToolsets(): List<ToolsetInfo> {
        return try {
            api.getToolsets().data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCapabilities(): String {
        return try {
            api.getCapabilities().string()
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    suspend fun getJobs(): String {
        return try {
            api.getJobs().string()
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    fun chatStream(
        sessionId: String,
        messages: List<Message>,
        model: String,
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ): Flow<String> = flow {
        val fullMessages = mutableListOf(Message(role = "system", content = systemPrompt))
        
        // Convert history to clean payload
        messages.forEach { msg ->
            fullMessages.add(msg.copy(timestamp = 0, imageUrl = null))
        }

        val userMessage = messages.last()
        chatDao.insertMessage(userMessage.toEntity(sessionId))

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
                    val data = line.substring(6).trim()
                    if (data.isNotEmpty() && data != "[DONE]") {
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

        if (fullContent.isNotEmpty()) {
            chatDao.insertMessage(Message(role = "assistant", content = fullContent.toString()).toEntity(sessionId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun clearHistory(sessionId: String) {
        chatDao.clearSessionHistory(sessionId)
    }

    suspend fun checkHealth(): Result<Unit> {
        return try {
            api.checkHealth()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performSelfDestruct(retentionPeriodMs: Long) {
        val threshold = System.currentTimeMillis() - retentionPeriodMs
        chatDao.deleteMessagesOlderThan(threshold)
    }
    
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}
