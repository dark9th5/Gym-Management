package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding token for auth endpoints (login, register, refresh)
        val url = originalRequest.url.toString()
        if (url.contains("/api/auth/login") ||
            url.contains("/api/auth/register") ||
            url.contains("/api/auth/refresh") ||
            url.contains("/api/auth/request-email-verification") ||
            url.contains("/api/auth/verify-email-code")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenManager.getAccessToken()
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
