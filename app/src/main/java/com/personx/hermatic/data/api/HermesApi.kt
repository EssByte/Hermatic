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

    // Responses API
    @POST("v1/responses")
    suspend fun createResponse(@Body request: ResponseCreateRequest): ResponseData

    @GET("v1/responses/{id}")
    suspend fun getResponse(@Path("id") id: String): ResponseData

    @DELETE("v1/responses/{id}")
    suspend fun deleteResponse(@Path("id") id: String): ResponseBody

    // Sessions API
    @GET("api/sessions")
    suspend fun getSessions(): SessionListResponse

    @POST("api/sessions")
    suspend fun createSession(@Body body: Map<String, String>): Session

    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): Session

    @PATCH("api/sessions/{id}")
    suspend fun updateSession(@Path("id") id: String, @Body body: SessionUpdateRequest): Session

    @DELETE("api/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): ResponseBody

    @GET("api/sessions/{id}/messages")
    suspend fun getSessionMessages(@Path("id") id: String): List<Message>

    @POST("api/sessions/{id}/fork")
    suspend fun forkSession(@Path("id") id: String, @Body body: SessionForkRequest): Session

    @Streaming
    @POST("api/sessions/{id}/chat/stream")
    suspend fun sessionChatStream(@Path("id") id: String, @Body request: SessionChatRequest): ResponseBody

    // Runs API
    @POST("v1/runs")
    suspend fun createRun(@Body request: ChatRequest): RunResponse

    @Streaming
    @GET("v1/runs/{id}/events")
    suspend fun getRunEvents(@Path("id") id: String): ResponseBody

    @POST("v1/runs/{id}/stop")
    suspend fun stopRun(@Path("id") id: String): StopRunResponse

    @POST("v1/runs/{id}/approval")
    suspend fun approveRun(@Path("id") id: String, @Body request: ApprovalRequest): ResponseBody

    // Jobs API
    @GET("api/jobs")
    suspend fun getJobs(): ResponseBody

    @GET("api/jobs/{id}")
    suspend fun getJob(@Path("id") id: String): ResponseBody

    @POST("api/jobs")
    suspend fun createJob(@Body request: JobCreateRequest): ResponseBody

    @PATCH("api/jobs/{id}")
    suspend fun updateJob(@Path("id") id: String, @Body request: JobUpdateRequest): ResponseBody

    @DELETE("api/jobs/{id}")
    suspend fun deleteJob(@Path("id") id: String): ResponseBody

    @POST("api/jobs/{id}/pause")
    suspend fun pauseJob(@Path("id") id: String): ResponseBody

    @POST("api/jobs/{id}/resume")
    suspend fun resumeJob(@Path("id") id: String): ResponseBody

    @POST("api/jobs/{id}/run")
    suspend fun runJob(@Path("id") id: String): ResponseBody

    @GET("health")
    suspend fun checkHealth(): ResponseBody

    @GET("health/detailed")
    suspend fun checkHealthDetailed(): HealthDetailed
}
