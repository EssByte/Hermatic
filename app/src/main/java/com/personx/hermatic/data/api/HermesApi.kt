package com.personx.hermatic.data.api

import com.personx.hermatic.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.*

interface HermesApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse

    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatCompletionsStream(@Body request: ChatRequest): ResponseBody

    @GET("v1/models")
    suspend fun getModels(): ModelListResponse

    @GET("v1/skills")
    suspend fun getSkills(): SkillListResponse

    @GET("v1/toolsets")
    suspend fun getToolsets(): ToolsetListResponse

    @GET("v1/capabilities")
    suspend fun getCapabilities(): ResponseBody

    // Sessions API
    @GET("api/sessions")
    suspend fun getSessions(): SessionListResponse

    @POST("api/sessions")
    suspend fun createSession(@Body body: Map<String, String>): Session

    @GET("api/sessions/{id}/messages")
    suspend fun getSessionMessages(@Path("id") id: String): List<Message>

    // Runs API
    @POST("v1/runs")
    suspend fun createRun(@Body request: ChatRequest): RunResponse

    @Streaming
    @GET("v1/runs/{id}/events")
    suspend fun getRunEvents(@Path("id") id: String): ResponseBody

    @GET("api/jobs")
    suspend fun getJobs(): ResponseBody

    @GET("health")
    suspend fun checkHealth(): ResponseBody
}
