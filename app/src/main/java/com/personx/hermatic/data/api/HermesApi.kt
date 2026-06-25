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

    @Streaming
    @POST("api/sessions/{id}/chat/stream")
    suspend fun sessionChatStream(@Path("id") id: String, @Body request: SessionChatRequest): ResponseBody

    // Runs API
    @POST("v1/runs")
    suspend fun createRun(@Body request: ChatRequest): RunResponse

    @Streaming
    @GET("v1/runs/{id}/events")
    suspend fun getRunEvents(@Path("id") id: String): ResponseBody

    @GET("api/jobs")
    suspend fun getJobs(): ResponseBody

    @POST("api/jobs")
    suspend fun createJob(@Body request: JobCreateRequest): ResponseBody

    @DELETE("api/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: String): ResponseBody

    @GET("health")
    suspend fun checkHealth(): ResponseBody

    @GET("health/detailed")
    suspend fun checkHealthDetailed(): HealthDetailed

    @POST("v1/runs/{id}/stop")
    suspend fun stopRun(@Path("id") id: String): StopRunResponse
}
