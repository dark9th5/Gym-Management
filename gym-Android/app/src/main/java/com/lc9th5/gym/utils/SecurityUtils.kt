package com.lc9th5.gym.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.lc9th5.gym.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest

/**
 * Security Utilities for Android App Protection
 * 
 * Features:
 * - Root Detection (Multiple methods)
 * - Emulator Detection (Comprehensive checks)
 * - Anti-Tampering (Signature verification)
 * - Debugger Detection
 * - Frida Detection (Anti-hooking)
 */
object SecurityUtils {

    private const val TAG = "SecurityUtils"
    
    // Expected app signature SHA-256 từ gym-release.jks keystore
    // Dùng để verify app không bị modify/repack
    private val EXPECTED_SIGNATURES: Set<String> = setOf(
        "692D48DDC1B0D6670F09A4D005" // SHA-256 của gym-release.jks
    )

    // ===============================================
    // ROOT DETECTION
    // ===============================================
    
    /**
     * Comprehensive root detection using multiple methods
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || 
               checkRootMethod2() || 
               checkRootMethod3() ||
               checkRootMethod4() ||
               checkRootMethod5()
    }

    // Method 1: Check build tags
    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    // Method 2: Check common root paths
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
            "/su/bin/su",
            "/su/bin",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99telecominfomern",
            "/system/lib/librt.so",
            "/system/lib/libsqprofile.so",
            "/system/bin/.ext/.su",
            "/system/etc/.has_su_daemon",
            "/system/etc/.installed_su_daemon",
            "/dev/com.koushikdutta.superuser.daemon/"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    // Method 3: Execute 'which su' command
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

    // Method 4: Check for root management apps
    private fun checkRootMethod4(): Boolean {
        val rootPackages = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )
        
        for (packageName in rootPackages) {
            try {
                val file = File("/data/data/$packageName")
                if (file.exists()) return true
            } catch (e: Exception) {
                // Ignore
            }
        }
        return false
    }

    // Method 5: Check for Magisk (advanced root)
    private fun checkRootMethod5(): Boolean {
        val magiskPaths = arrayOf(
            "/sbin/.magisk",
            "/sbin/.core",
            "/data/adb/magisk",
            "/data/adb/modules"
        )
        for (path in magiskPaths) {
            if (File(path).exists()) return true
        }
        
        // Check for MagiskHide
        return try {
            val process = Runtime.getRuntime().exec("getprop ro.boot.vbmeta.device_state")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor()
            result?.contains("unlocked") == true
        } catch (e: Exception) {
            false
        }
    }

    // ===============================================
    // EMULATOR DETECTION
    // ===============================================
    
    /**
     * Comprehensive emulator detection
     */
    fun isEmulator(): Boolean {
        return checkEmulatorBuild() || 
               checkEmulatorHardware() || 
               checkEmulatorSensors() ||
               checkEmulatorFiles()
    }

    private fun checkEmulatorBuild(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.FINGERPRINT.contains("test-keys") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk_google") ||
                Build.PRODUCT.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("sdk_x86") ||
                Build.PRODUCT.contains("sdk_gphone") ||
                Build.PRODUCT.contains("vbox86p") ||
                Build.PRODUCT.contains("emulator") ||
                Build.PRODUCT.contains("simulator") ||
                Build.BOARD == "QC_Reference_Phone" ||
                Build.HOST.startsWith("Build") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    }

    private fun checkEmulatorHardware(): Boolean {
        return Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.HARDWARE.contains("nox") ||
                Build.HARDWARE.contains("vbox")
    }

    @SuppressLint("HardwareIds")
    private fun checkEmulatorSensors(): Boolean {
        // Kiểm tra ANDROID_ID
        return try {
            false // Không check vì cần context
        } catch (e: Exception) {
            false
        }
    }

    private fun checkEmulatorFiles(): Boolean {
        val emulatorFiles = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/socket/genyd",
            "/dev/socket/baseband_genyd"
        )
        for (file in emulatorFiles) {
            if (File(file).exists()) return true
        }
        return false
    }

    // ===============================================
    // ANTI-TAMPERING (Signature Verification)
    // ===============================================
    
    /**
     * Verify app signature hasn't been modified
     * Returns true if signature is valid or verification is disabled
     */
    @SuppressLint("PackageManagerGetSignatures")
    fun isSignatureValid(context: Context): Boolean {
        
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.any { signature ->
                val signatureHash = sha256(signature.toByteArray())
                EXPECTED_SIGNATURES.contains(signatureHash)
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            false
        }
    }
    
    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }.uppercase()
    }

    // ===============================================
    // DEBUGGER DETECTION
    // ===============================================
    
    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected() || 
               android.os.Debug.waitingForDebugger()
    }

    // ===============================================
    // FRIDA DETECTION (Anti-Hooking)
    // ===============================================
    
    /**
     * Basic Frida detection
     */
    fun isFridaDetected(): Boolean {
        return checkFridaPort() || checkFridaFiles()
    }

    private fun checkFridaPort(): Boolean {
        return try {
            // Frida default port is 27042
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 100)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkFridaFiles(): Boolean {
        val fridaPaths = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/sdcard/frida-server"
        )
        for (path in fridaPaths) {
            if (File(path).exists()) return true
        }
        return false
    }

    // ===============================================
    // COMPREHENSIVE SECURITY CHECK
    // ===============================================
    
    /**
     * Performs all security checks and returns detailed result
     */
    fun performSecurityChecks(context: Context): SecurityCheckResult {
        val rooted = isDeviceRooted()
        val emulator = isEmulator()
        val signatureValid = isSignatureValid(context)
        val debuggerAttached = isDebuggerAttached()
        val fridaDetected = isFridaDetected()

        // Log results
        Log.d(TAG, """
            Security Check Results:
            - Rooted: $rooted
            - Emulator: $emulator
            - Signature Valid: $signatureValid
            - Debugger Attached: $debuggerAttached
            - Frida Detected: $fridaDetected
            - Debug Build: ${BuildConfig.DEBUG}
        """.trimIndent())

        // In DEBUG build: allow but warn
        if (BuildConfig.DEBUG) {
            if (rooted) Log.w(TAG, "Device rooted (DEBUG) - allowing")
            if (emulator) Log.w(TAG, "Running on emulator (DEBUG) - allowing")
            if (debuggerAttached) Log.w(TAG, "Debugger attached (DEBUG) - allowing")
            
            return SecurityCheckResult(
                isSecure = true,
                rooted = rooted,
                emulator = emulator,
                signatureValid = signatureValid,
                debuggerAttached = debuggerAttached,
                fridaDetected = fridaDetected,
                message = "DEBUG build - all checks passed"
            )
        }

        // In RELEASE build: enforce security
        val issues = mutableListOf<String>()
        
        if (rooted) issues.add("Rooted device detected")
        if (fridaDetected) issues.add("Frida hooking framework detected")
        if (debuggerAttached) issues.add("Debugger attached")
        if (!signatureValid) {
            issues.add("Invalid app signature")
        }
        // Note: Emulator không block trong release để cho phép test nội bộ
        
        val isSecure = issues.isEmpty()
        
        return SecurityCheckResult(
            isSecure = isSecure,
            rooted = rooted,
            emulator = emulator,
            signatureValid = signatureValid,
            debuggerAttached = debuggerAttached,
            fridaDetected = fridaDetected,
            message = if (isSecure) "All security checks passed" else issues.joinToString(", ")
        )
    }
    
    /**
     * Simple check that returns boolean (for backward compatibility)
     */
    fun performSecurityChecks_Legacy(context: Context): Boolean {
        return performSecurityChecks(context).isSecure
    }
}

/**
 * Data class for detailed security check results
 */
data class SecurityCheckResult(
    val isSecure: Boolean,
    val rooted: Boolean,
    val emulator: Boolean,
    val signatureValid: Boolean,
    val debuggerAttached: Boolean,
    val fridaDetected: Boolean,
    val message: String
)
