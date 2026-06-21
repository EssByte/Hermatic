package com.personx.hermatic.data.api

import com.personx.hermatic.security.SecurityManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val securityManager: SecurityManager) {
    val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Increased to 120s for slow LLM responses
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS) // Total call timeout
        .addInterceptor(AuthInterceptor(securityManager))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // Using a function to build the API ensures we use the latest URL from SecurityManager
    fun createHermesApi(): HermesApi {
        return Retrofit.Builder()
            .baseUrl(securityManager.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HermesApi::class.java)
    }
    
    // For convenience
    val hermesApi: HermesApi get() = createHermesApi()
}
