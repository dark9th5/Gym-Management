package com.lc9th5.gym.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.lc9th5.gym.BuildConfig
import java.io.File

object SecurityUtils {

    private const val TAG = "SecurityUtils"

    /**
     * Checks if the device is rooted.
     * This is a basic check and may not detect all rooting methods.
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val inputStream = process.inputStream
            inputStream.readBytes().isNotEmpty()
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * Checks if the app is running on an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Performs security checks and returns true if the environment is secure.
     *
     * In bản phát triển (DEBUG), ta nới lỏng kiểm tra để tránh app tự thoát
     * trên thiết bị/emulator hợp lệ, đồng thời log rõ lý do.
     */
    fun performSecurityChecks(context: Context): Boolean {
        val rooted = isDeviceRooted()
        val emulator = isEmulator()

        // Luôn log lại kết quả để dễ debug
        Log.d(TAG, "performSecurityChecks: rooted=$rooted, emulator=$emulator, debug=${BuildConfig.DEBUG}")

        // Trong build DEBUG: chỉ cảnh báo, không chặn app
        if (BuildConfig.DEBUG) {
            if (rooted) {
                Log.w(TAG, "Device appears to be rooted (DEBUG build) - allowing for development use")
            }
            if (emulator) {
                Log.w(TAG, "App is running on an emulator (DEBUG build) - allowing for development use")
            }
            return true
        }

        // Trong build RELEASE: tùy chiến lược, ở đây ta chỉ chặn thiết bị root,
        // nhưng vẫn cho phép chạy trên emulator nếu bạn cần test nội bộ.
        if (rooted) {
            Log.e(TAG, "Security check failed: rooted device detected (RELEASE build)")
            return false
        }

        // Nếu bạn thực sự muốn chặn emulator trên bản release, bỏ comment đoạn dưới:
        // if (emulator) {
        //     Log.e(TAG, "Security check failed: emulator detected (RELEASE build)")
        //     return false
        // }

        return true
    }
}
