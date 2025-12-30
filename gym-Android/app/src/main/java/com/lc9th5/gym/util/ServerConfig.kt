package com.lc9th5.gym.util


object ServerConfig {
    
    const val BASE_URL = "https://tetratomic-nonexistentially-rigoberto.ngrok-free.dev"
    
    /**
     * URL cho Guidance/Hướng dẫn tập luyện
     */
    const val GUIDANCE_BASE_URL = BASE_URL
    
    /**
     * Danh sách các production domains (HTTPS bắt buộc)
     */
    private val PRODUCTION_DOMAINS = listOf(
        "ngrok-free.dev",
        "ngrok.io",
        "supabase.co",
        // Thêm production domain của bạn ở đây
        // "api.yourdomain.com"
    )
    
    /**
     * Danh sách các local development IPs (cho phép HTTP)
     */
    private val LOCAL_DEVELOPMENT_PATTERNS = listOf(
        "10.0.2.2",      // Android Emulator
        "localhost",
        "127.0.0.1",
        "192.168."       // Local network
    )
    
    /**
     * Kiểm tra xem có đang dùng emulator không
     */
    fun isEmulator(): Boolean {
        return BASE_URL.contains("10.0.2.2")
    }
    
    /**
     * Kiểm tra xem URL có phải là production URL không
     * Production URLs yêu cầu HTTPS và certificate validation
     */
    fun isProductionUrl(): Boolean {
        return PRODUCTION_DOMAINS.any { domain -> 
            BASE_URL.contains(domain) 
        }
    }
    
    /**
     * Kiểm tra xem URL có phải local development không
     */
    fun isLocalDevelopment(): Boolean {
        return LOCAL_DEVELOPMENT_PATTERNS.any { pattern ->
            BASE_URL.contains(pattern)
        }
    }
    
    /**
     * Kiểm tra xem URL có sử dụng HTTPS không
     */
    fun isSecure(): Boolean {
        return BASE_URL.startsWith("https://")
    }
    
    /**
     * Lấy hostname từ BASE_URL
     */
    fun getHostname(): String {
        return try {
            java.net.URL(BASE_URL).host
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Log thông tin cấu hình (dùng để debug)
     */
    fun logConfig() {
        println("=== Server Configuration ===")
        println("Base URL: $BASE_URL")
        println("Hostname: ${getHostname()}")
        println("Is Secure (HTTPS): ${isSecure()}")
        println("Is Production: ${isProductionUrl()}")
        println("Is Local Dev: ${isLocalDevelopment()}")
        println("Is Emulator: ${isEmulator()}")
        println("==========================")
    }
}
