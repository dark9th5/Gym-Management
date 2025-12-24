package com.lc9th5.gym.util

/**
 * Cấu hình địa chỉ server cho ứng dụng
 * 
 * BẢO MẬT ĐƯỜNG TRUYỀN (Transport Layer Security):
 * - Production: Sử dụng HTTPS với certificate validation
 * - Development: Có thể dùng HTTP cho local IPs
 * 
 * QUAN TRỌNG: Thay đổi SERVER_IP bên dưới thành IP thực tế của máy chạy backend
 * 
 * Để tìm IP:
 * - Windows: chạy lệnh `ipconfig` trong Command Prompt/PowerShell
 * - macOS/Linux: chạy lệnh `ifconfig` hoặc `ip addr`
 * - Android Emulator: sử dụng "10.0.2.2" (trỏ đến localhost của máy host)
 */
object ServerConfig {
    /**
     * ⚠️ THAY ĐỔI URL NÀY THEO MÔI TRƯỜNG CỦA BẠN ⚠️
     * 
     * Ví dụ:
     * - Ngrok HTTPS: "https://xxx.ngrok-free.dev" (khuyến nghị cho development)
     * - Production: "https://api.yourdomain.com"
     * - Local HTTP (không khuyến nghị): "http://192.168.x.x:8080"
     */
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
