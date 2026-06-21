package com.personx.hermatic.data.api

import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.ChatResponse
import com.personx.hermatic.data.model.ModelListResponse
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

    @GET("health")
    suspend fun checkHealth(): okhttp3.ResponseBody
}
