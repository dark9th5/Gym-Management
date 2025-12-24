package com.lc9th5.gym

import android.app.Application
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.remote.ApiClient
import com.lc9th5.gym.utils.SecurityUtils
import com.lc9th5.gym.utils.SupabaseStorageHelper

class GymApplication : Application() {

    companion object {
        val SUPABASE_URL = BuildConfig.SUPABASE_URL
        val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
    }

    lateinit var tokenManager: TokenManager
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var supabaseStorageHelper: SupabaseStorageHelper
        private set

    override fun onCreate() {
        super.onCreate()

        // Perform security checks. Only hard-stop in non-debug (release) builds.
        val securityOk = SecurityUtils.performSecurityChecks(this)
        if (!securityOk) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("GymApplication", "Security checks failed (DEBUG build) - continuing for investigation")
            } else {
                android.util.Log.e("GymApplication", "Security checks failed (RELEASE build) - terminating process")
                android.os.Process.killProcess(android.os.Process.myPid())
                return
            }
        }

        try {
            tokenManager = TokenManager(this)
            apiClient = ApiClient.getInstance(tokenManager)
            supabaseStorageHelper = SupabaseStorageHelper(SUPABASE_URL, SUPABASE_ANON_KEY, this)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("GymApplication", "Initialization error in onCreate", e)
            // Avoid immediate crash in debug so we can inspect further; crash in release for visibility.
            if (!BuildConfig.DEBUG) throw e
        }

        android.util.Log.i("GymApplication", "GymApplication initialized. securityOk=$securityOk debug=${BuildConfig.DEBUG}")
    }
}
