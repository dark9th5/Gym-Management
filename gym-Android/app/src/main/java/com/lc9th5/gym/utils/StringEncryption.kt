package com.lc9th5.gym.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * String Encryption Utility using Android Keystore
 * 
 * Provides secure encryption for sensitive strings like API keys,
 * tokens, and user data using Android Keystore System.
 * 
 * Features:
 * - AES-256-GCM encryption
 * - Hardware-backed key storage (when available)
 * - Keys never leave the device
 */
object StringEncryption {
    
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "GymAppSecureKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    
    /**
     * Encrypts a string using Android Keystore
     * @param plainText The text to encrypt
     * @return Base64 encoded encrypted string (IV + ciphertext)
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV (12 bytes) + ciphertext
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    /**
     * Decrypts an encrypted string
     * @param encryptedText Base64 encoded encrypted string
     * @return Original plain text
     */
    fun decrypt(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        
        // Extract IV (first 12 bytes) and ciphertext
        val iv = combined.copyOfRange(0, 12)
        val cipherText = combined.copyOfRange(12, combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), gcmSpec)
        
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }
    
    /**
     * Safe decrypt - returns original text if decryption fails
     */
    fun safeDecrypt(text: String): String {
        return try {
            if (isEncrypted(text)) decrypt(text) else text
        } catch (e: Exception) {
            text
        }
    }
    
    /**
     * Check if text looks like encrypted data
     */
    fun isEncrypted(text: String): Boolean {
        return try {
            val decoded = Base64.decode(text, Base64.NO_WRAP)
            decoded.size > 12 // At least IV (12) + some ciphertext
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets existing key or creates a new one in Android Keystore
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        // Return existing key if available
        keyStore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey
        }
        
        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Không yêu cầu xác thực mỗi lần
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Delete the encryption key (use with caution!)
     */
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        keyStore.deleteEntry(KEY_ALIAS)
    }
}
