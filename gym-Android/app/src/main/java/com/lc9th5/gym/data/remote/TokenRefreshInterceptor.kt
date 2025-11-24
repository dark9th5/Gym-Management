package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.model.AuthResponse
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TokenRefreshInterceptor(private val tokenManager: TokenManager) : Interceptor {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.108:8080/") // Synced with ApiClient BASE_URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val authApi = retrofit.create(AuthApi::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // If response is 401 and we have refresh token, try to refresh
        if (response.code == 401 && tokenManager.getRefreshToken() != null) {
            synchronized(this) {
                // Double-check after acquiring lock
                val currentToken = tokenManager.getAccessToken()
                if (currentToken != null && tokenManager.isTokenExpired()) {
                    val refreshSuccess = refreshToken()
                    if (refreshSuccess) {
                        // Retry the original request with new token
                        val newToken = tokenManager.getAccessToken()
                        val newRequest = originalRequest.newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", "Bearer $newToken")
                            .build()
                        response.close() // Close the original response
                        return chain.proceed(newRequest)
                    }
                }
            }
        }

        return response
    }

    private fun refreshToken(): Boolean {
        val refreshToken = tokenManager.getRefreshToken() ?: return false

        val latch = CountDownLatch(1)
        var success = false

        val call = authApi.refreshToken("Bearer $refreshToken")
        call.enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: retrofit2.Response<AuthResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { authResponse ->
                        tokenManager.saveAuthResponse(authResponse)
                        success = true
                    }
                }
                latch.countDown()
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                latch.countDown()
            }
        })

        // Wait for response with timeout
        return try {
            latch.await(10, TimeUnit.SECONDS)
            success
        } catch (e: InterruptedException) {
            false
        }
    }

    private interface AuthApi {
        @POST("/api/auth/refresh")
        fun refreshToken(@Header("Authorization") authHeader: String): Call<AuthResponse>
    }
}
