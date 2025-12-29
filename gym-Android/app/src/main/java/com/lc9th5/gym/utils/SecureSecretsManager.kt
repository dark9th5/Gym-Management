package com.lc9th5.gym.utils

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure Secrets Manager
 * 
 * Quản lý và mã hóa các API keys, tokens, và secrets nhạy cảm
 * Sử dụng Android Keystore + EncryptedSharedPreferences
 * 
 * CÁCH SỬ DỤNG:
 * 1. Lần đầu chạy app: secrets từ BuildConfig được mã hóa và lưu
 * 2. Các lần sau: secrets được đọc từ storage đã mã hóa
 * 3. Trong production/release: BuildConfig không chứa secrets thật
 */
object SecureSecretsManager {
    
    private const val TAG = "SecureSecretsManager"
    private const val PREFS_FILE = "secure_secrets"
    
    // Keys
    private const val KEY_SUPABASE_URL = "supabase_url"
    private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
    private const val KEY_INITIALIZED = "initialized"
    
    private var encryptedPrefs: android.content.SharedPreferences? = null
    
    /**
     * Khởi tạo secure storage
     * Gọi trong Application.onCreate()
     */
    fun initialize(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // Lần đầu chạy: import secrets từ BuildConfig
            if (!isInitialized()) {
                initializeSecrets(context)
            }
            
            Log.d(TAG, "SecureSecretsManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SecureSecretsManager", e)
        }
    }
    
    private fun isInitialized(): Boolean {
        return encryptedPrefs?.getBoolean(KEY_INITIALIZED, false) ?: false
    }
    
    /**
     * Import secrets từ BuildConfig (chỉ chạy 1 lần)
     * 
     * Trong production, bạn nên:
     * 1. Xóa secrets khỏi BuildConfig
     * 2. Inject secrets qua secure method khác (ví dụ: từ server khi login)
     */
    private fun initializeSecrets(context: Context) {
        try {
            // Import từ BuildConfig
            val supabaseUrl = com.lc9th5.gym.BuildConfig.SUPABASE_URL
            val supabaseKey = com.lc9th5.gym.BuildConfig.SUPABASE_ANON_KEY
            
            encryptedPrefs?.edit()?.apply {
                putString(KEY_SUPABASE_URL, supabaseUrl)
                putString(KEY_SUPABASE_ANON_KEY, supabaseKey)
                putBoolean(KEY_INITIALIZED, true)
                apply()
            }
            
            Log.d(TAG, "Secrets imported and encrypted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize secrets", e)
        }
    }
    
    /**
     * Lấy Supabase URL (đã giải mã)
     */
    fun getSupabaseUrl(): String {
        return encryptedPrefs?.getString(KEY_SUPABASE_URL, "") ?: ""
    }
    
    /**
     * Lấy Supabase Anon Key (đã giải mã)
     */
    fun getSupabaseAnonKey(): String {
        return encryptedPrefs?.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
    }
    
    /**
     * Cập nhật secret (dùng khi server gửi secret mới)
     */
    fun updateSecret(key: String, value: String) {
        encryptedPrefs?.edit()?.putString(key, value)?.apply()
    }
    
    /**
     * Xóa tất cả secrets (dùng khi logout)
     */
    fun clearSecrets() {
        encryptedPrefs?.edit()?.clear()?.apply()
    }
    
    /**
     * Kiểm tra xem secrets có sẵn không
     */
    fun hasSecrets(): Boolean {
        return getSupabaseUrl().isNotBlank() && getSupabaseAnonKey().isNotBlank()
    }
}
