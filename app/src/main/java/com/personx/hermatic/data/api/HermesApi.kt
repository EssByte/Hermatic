package com.personx.hermatic.data.api

import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.ChatResponse
import com.personx.hermatic.data.model.ModelListResponse
import com.personx.hermatic.data.model.SkillListResponse
import com.personx.hermatic.data.model.ToolsetListResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

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
    suspend fun getCapabilities(): okhttp3.ResponseBody

    @GET("api/sessions")
    suspend fun getSessions(): okhttp3.ResponseBody

    @GET("api/jobs")
    suspend fun getJobs(): okhttp3.ResponseBody

    @GET("health")
    suspend fun checkHealth(): ResponseBody
}
