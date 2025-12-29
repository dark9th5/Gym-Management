package com.lc9th5.gym

import android.app.Application
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.remote.ApiClient
import com.lc9th5.gym.utils.SecureSecretsManager
import com.lc9th5.gym.utils.SecurityUtils
import com.lc9th5.gym.utils.SupabaseStorageHelper

class GymApplication : Application() {

    companion object {
        /**
         * Lấy Supabase URL từ SecureSecretsManager (đã mã hóa)
         * Fallback sang BuildConfig nếu chưa khởi tạo
         */
        fun getSupabaseUrl(): String {
            return if (SecureSecretsManager.hasSecrets()) {
                SecureSecretsManager.getSupabaseUrl()
            } else {
                BuildConfig.SUPABASE_URL
            }
        }
        
        /**
         * Lấy Supabase Anon Key từ SecureSecretsManager (đã mã hóa)
         * Fallback sang BuildConfig nếu chưa khởi tạo
         */
        fun getSupabaseAnonKey(): String {
            return if (SecureSecretsManager.hasSecrets()) {
                SecureSecretsManager.getSupabaseAnonKey()
            } else {
                BuildConfig.SUPABASE_ANON_KEY
            }
        }
        
        // Backward compatibility - sẽ deprecated
        @Deprecated("Use getSupabaseUrl() instead", ReplaceWith("getSupabaseUrl()"))
        val SUPABASE_URL: String get() = getSupabaseUrl()
        
        @Deprecated("Use getSupabaseAnonKey() instead", ReplaceWith("getSupabaseAnonKey()"))
        val SUPABASE_ANON_KEY: String get() = getSupabaseAnonKey()
    }

    lateinit var tokenManager: TokenManager
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var supabaseStorageHelper: SupabaseStorageHelper
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Khởi tạo Secure Secrets Manager trước
        SecureSecretsManager.initialize(this)
        android.util.Log.d("GymApplication", "SecureSecretsManager initialized")

        // 2. Perform security checks
        val securityResult = SecurityUtils.performSecurityChecks(this)
        if (!securityResult.isSecure) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("GymApplication", "Security checks failed (DEBUG build) - ${securityResult.message}")
            } else {
                android.util.Log.e("GymApplication", "Security checks failed (RELEASE build) - ${securityResult.message}")
                android.os.Process.killProcess(android.os.Process.myPid())
                return
            }
        }

        // 3. Initialize app components
        try {
            tokenManager = TokenManager(this)
            apiClient = ApiClient.getInstance(tokenManager)
            
            // Sử dụng secrets đã mã hóa từ SecureSecretsManager
            supabaseStorageHelper = SupabaseStorageHelper(
                getSupabaseUrl(), 
                getSupabaseAnonKey(), 
                this
            )
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("GymApplication", "Initialization error in onCreate", e)
            if (!BuildConfig.DEBUG) throw e
        }

        android.util.Log.i("GymApplication", "GymApplication initialized. secure=${securityResult.isSecure} debug=${BuildConfig.DEBUG}")
    }
}
