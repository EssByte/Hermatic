package com.personx.hermatic.data.api

import com.personx.hermatic.data.model.ChatRequest
import com.personx.hermatic.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface HermesApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse
}
