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

    suspend fun insertMessage(entity: ChatMessageEntity) {
        chatDao.insertMessage(entity)
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

    // ── Jobs API ──

    suspend fun getJobs(): String {
        return try {
            api.getJobs().string()
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    suspend fun getJob(id: String): Result<String> {
        return try {
            Result.success(api.getJob(id).string())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createJob(prompt: String, schedule: String = "*/30 * * * *"): Result<String> {
        return try {
            val response = api.createJob(JobCreateRequest(prompt = prompt, schedule = schedule))
            Result.success(response.string())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateJob(id: String, prompt: String? = null, schedule: String? = null): Result<Unit> {
        return try {
            api.updateJob(id, JobUpdateRequest(prompt = prompt, schedule = schedule))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteJob(id: String): Result<Unit> {
        return try {
            api.deleteJob(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pauseJob(id: String): Result<Unit> {
        return try { api.pauseJob(id); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun resumeJob(id: String): Result<Unit> {
        return try { api.resumeJob(id); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun runJob(id: String): Result<Unit> {
        return try { api.runJob(id); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    // ── Responses API ──

    suspend fun createResponse(input: String, instructions: String? = null, previousResponseId: String? = null): Result<ResponseData> {
        return try {
            Result.success(api.createResponse(ResponseCreateRequest(input = input, instructions = instructions, previous_response_id = previousResponseId)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getResponse(id: String): Result<ResponseData> {
        return try {
            Result.success(api.getResponse(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteResponse(id: String): Result<Unit> {
        return try { api.deleteResponse(id); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    // ── Sessions API ──

    suspend fun getServerSession(id: String): Result<Session> {
        return try { Result.success(api.getSession(id)) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateServerSession(id: String, title: String? = null, endReason: String? = null): Result<Session> {
        return try { Result.success(api.updateSession(id, SessionUpdateRequest(title = title, end_reason = endReason))) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteServerSession(id: String): Result<Unit> {
        return try { api.deleteSession(id); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun forkSession(id: String, title: String? = null): Result<Session> {
        return try { Result.success(api.forkSession(id, SessionForkRequest(title = title))) } catch (e: Exception) { Result.failure(e) }
    }

    // ── Runs API ──

    suspend fun approveRun(runId: String, approved: Boolean, reason: String? = null): Result<Unit> {
        return try { api.approveRun(runId, ApprovalRequest(approved = approved, reason = reason)); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    // ── Chat Streaming ──

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

    fun sessionChatStream(
        sessionId: String,
        input: String,
        instructions: String?
    ): Flow<ChatStreamEvent> = flow {
        val request = SessionChatRequest(input = input, instructions = instructions)
        val responseBody = api.sessionChatStream(sessionId, request)

        responseBody.byteStream().bufferedReader().useLines { lines ->
            var currentEvent: String? = null
            lines.forEach { line ->
                when {
                    line.startsWith("event: ") -> {
                        currentEvent = line.substring(7).trim()
                    }
                    line.startsWith("data: ") -> {
                        val data = line.substring(6).trim()
                        if (data.isNotEmpty() && currentEvent != null) {
                            val event = currentEvent!!
                            currentEvent = null
                            try {
                                val evData = json.decodeFromString<ChatStreamEventData>(data)
                                when (event) {
                                    "assistant.delta" -> {
                                        evData.content?.let { emit(ChatStreamEvent.TextDelta(it)) }
                                    }
                                    "tool.started" -> {
                                        val name = evData.name ?: "unknown"
                                        val args = evData.arguments ?: "{}"
                                        val callId = evData.call_id ?: "call_${System.currentTimeMillis()}"
                                        emit(ChatStreamEvent.ToolStarted(name, args, callId))
                                    }
                                    "tool.completed" -> {
                                        val callId = evData.call_id ?: ""
                                        val output = evData.output ?: ""
                                        emit(ChatStreamEvent.ToolCompleted(callId, output))
                                    }
                                    "run.completed" -> {
                                        emit(ChatStreamEvent.RunCompleted)
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun clearHistory(sessionId: String) {
        chatDao.clearSessionHistory(sessionId)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessageById(messageId)
    }

    suspend fun editMessage(messageId: Long, newContent: String) {
        chatDao.updateMessageContent(messageId, newContent)
    }

    suspend fun checkHealth(): Result<Unit> {
        return try {
            api.checkHealth()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealthDetailed(): Result<HealthDetailed> {
        return try {
            Result.success(api.checkHealthDetailed())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopRun(runId: String): Result<Unit> {
        return try {
            api.stopRun(runId)
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
