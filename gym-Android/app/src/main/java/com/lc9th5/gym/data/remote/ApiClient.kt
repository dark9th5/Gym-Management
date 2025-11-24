package com.lc9th5.gym.data.remote

import com.lc9th5.gym.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val tokenManager: TokenManager) {
    // CONFIGURATION: Change based on your testing environment
    // 1. For Android Emulator: Use "http://10.0.2.2:8080"
    // 2. For Physical Device: Use "http://192.168.0.102:8080" (your computer's IP)
    // 3. For Production: Use your actual server URL

    // Current setting: Physical Device (điện thoại thật)
    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = AuthInterceptor(tokenManager)
    private val tokenRefreshInterceptor = TokenRefreshInterceptor(tokenManager)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(tokenRefreshInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
    val familyApiService: FamilyApiService = retrofit.create(FamilyApiService::class.java)
    val guidanceApiService: GuidanceApiService = retrofit.create(GuidanceApiService::class.java)

    companion object {
        // Use your PC IPv4 so a real device on same Wi-Fi can reach the backend
        private const val BASE_URL = "http://192.168.0.108:8080/"

        private var instance: ApiClient? = null

        fun getInstance(tokenManager: TokenManager): ApiClient {
            return instance ?: ApiClient(tokenManager).also { instance = it }
        }
    }
}
