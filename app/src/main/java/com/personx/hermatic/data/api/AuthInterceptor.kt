package com.personx.hermatic.data.api

import com.personx.hermatic.security.SecurityManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val securityManager: SecurityManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = securityManager.getApiKey()
        val request = chain.request().newBuilder()
        if (apiKey != null) {
            request.addHeader("Authorization", "Bearer $apiKey")
        }
        return chain.proceed(request.build())
    }
}
