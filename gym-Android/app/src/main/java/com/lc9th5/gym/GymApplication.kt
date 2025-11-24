package com.lc9th5.gym

import android.app.Application
import com.lc9th5.gym.data.local.TokenManager
import com.lc9th5.gym.data.remote.ApiClient

class GymApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var apiClient: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        apiClient = ApiClient.getInstance(tokenManager)
    }
}
