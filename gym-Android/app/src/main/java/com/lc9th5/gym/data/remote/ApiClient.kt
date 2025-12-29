package com.lc9th5.gym.data.remote

import android.util.Log
import com.lc9th5.gym.BuildConfig
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.util.ServerConfig
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ApiClient với bảo mật đường truyền (Transport Layer Security)
 * 
 * Security features:
 * - TLS 1.2/1.3 enforcement
 * - Certificate validation (không trust all certs)
 * - Optional certificate pinning cho production
 * - Secure connection specs
 */
class ApiClient(private val tokenManager: TokenManager) {
    
    companion object {
        private const val TAG = "ApiClient"
        private var instance: ApiClient? = null

        fun getInstance(tokenManager: TokenManager): ApiClient {
            return instance ?: ApiClient(tokenManager).also { instance = it }
        }
        
        /**
         * Certificate pins cho production
         * 
         * ⚠️ HƯỚNG DẪN SỬ DỤNG:
         * 1. Chỉ bật pinning cho PRODUCTION server (không phải ngrok)
         * 2. Luôn có ít nhất 2 pins (primary + backup) để tránh bị lock out khi renew cert
         * 3. Cập nhật backup pin TRƯỚC khi certificate hết hạn
         * 
         * Để lấy pin của domain, chạy:
         * openssl s_client -servername <domain> -connect <domain>:443 2>/dev/null | \
         *   openssl x509 -pubkey -noout | \
         *   openssl pkey -pubin -outform der | \
         *   openssl dgst -sha256 -binary | \
         *   openssl enc -base64
         * 
         * VÍ DỤ CHO PRODUCTION:
         * "api.gymapp.vn" to listOf(
         *     "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Primary cert
         *     "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Backup cert
         * )
         */
        private val CERTIFICATE_PINS: Map<String, List<String>> = mapOf(
            // ========== PRODUCTION PINS ==========
            // Uncomment và thêm domain production của bạn ở đây:
            // "api.gymapp.vn" to listOf(
            //     "YOUR_PRIMARY_PIN_HERE=",
            //     "YOUR_BACKUP_PIN_HERE="
            // ),
            
            // ========== KHÔNG NÊN PIN CHO NGROK ==========
            // Ngrok certificate tự động renew mỗi 90 ngày
            // → App sẽ không kết nối được sau khi renew
            "*.ngrok-free.dev" to listOf<String>(), // Để trống = không pin
            
            // ========== SUPABASE (Cloudflare) ==========
            // Supabase sử dụng Cloudflare SSL
            // Các pin dưới đây là Cloudflare Root CA pins (ổn định hơn leaf cert)
            "*.supabase.co" to listOf(
                // Cloudflare Inc ECC CA-3
                "Wf2blGUPOnPwtGC3SiS7Tqjx1+k3QGIhzrG3v88t7go=",
                // DigiCert Global Root CA (backup)
                "r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E="
            )
        )
    }

    // Logging interceptor - chỉ log chi tiết trong debug builds
    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE // Không log trong production
        }
    }

    private val authInterceptor = AuthInterceptor(tokenManager)
    private val tokenRefreshInterceptor = TokenRefreshInterceptor(tokenManager)

    /**
     * Tạo OkHttpClient bảo mật
     * - Sử dụng system trust store (không trust all certs)
     * - Enforce TLS 1.2+
     * - Optional certificate pinning
     */
    private fun createSecureOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // Enforce modern TLS versions
        builder.connectionSpecs(listOf(
            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                .build(),
            // Fallback cho local HTTP development
            ConnectionSpec.CLEARTEXT
        ))
        
        // Certificate pinning cho production
        if (!BuildConfig.DEBUG && ServerConfig.isProductionUrl()) {
            try {
                val certificatePinner = createCertificatePinner()
                if (certificatePinner != null) {
                    builder.certificatePinner(certificatePinner)
                    Log.d(TAG, "Certificate pinning enabled for production")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup certificate pinning: ${e.message}")
            }
        }
        
        // Add ngrok header for free tier (bypass browser warning)
        builder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("ngrok-skip-browser-warning", "true")
                .build()
            chain.proceed(request)
        }
        
        return builder.build()
    }
    
    /**
     * Tạo certificate pinner cho các domain quan trọng
     */
    private fun createCertificatePinner(): CertificatePinner? {
        val pinnerBuilder = CertificatePinner.Builder()
        var hasPins = false
        
        CERTIFICATE_PINS.forEach { (domain, pins) ->
            pins.forEach { pin ->
                pinnerBuilder.add(domain, "sha256/$pin")
                hasPins = true
            }
        }
        
        return if (hasPins) pinnerBuilder.build() else null
    }

    // Sử dụng secure client thay vì unsafe client
    private val client: OkHttpClient = createSecureOkHttpClient()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ServerConfig.BASE_URL + "/api/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
    val guidanceApiService: GuidanceApiService = retrofit.create(GuidanceApiService::class.java)
    val workoutApiService: WorkoutApiService = retrofit.create(WorkoutApiService::class.java)
    val chatApiService: ChatApiService = retrofit.create(ChatApiService::class.java)
}
