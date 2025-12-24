package com.lc9th5.gym.utils

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SupabaseStorageHelper(
    private val supabaseUrl: String,
    private val supabaseKey: String,
    private val context: Context
) {
    private val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
        install(Storage)
    }

    suspend fun uploadVideo(uri: Uri, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            validateFileSize(context, uri, MAX_VIDEO_SIZE_BYTES, "Video vượt quá dung lượng tối đa 50MB")
            val bytes = uri.toByteArray(context)
            val path = "videos/${UUID.randomUUID()}_$fileName"
            supabase.storage.from("videos").upload(path, bytes)
            supabase.storage.from("videos").publicUrl(path)
        }
    }

    suspend fun uploadImage(uri: Uri, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            validateFileSize(context, uri, MAX_IMAGE_SIZE_BYTES, "Hình ảnh vượt quá dung lượng tối đa 5MB")
            val bytes = uri.toByteArray(context)
            val path = "images/${UUID.randomUUID()}_$fileName"
            supabase.storage.from("images").upload(path, bytes)
            supabase.storage.from("images").publicUrl(path)
        }
    }

    suspend fun deleteFile(url: String): Boolean {
        return try {
            val path = url.substringAfter("storage/v1/object/public/")
            val bucket = path.substringBefore("/")
            val filePath = path.substringAfter("/")
            supabase.storage.from(bucket).delete(filePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// Extension function to convert Uri to ByteArray
private fun Uri.toByteArray(context: Context): ByteArray {
    return context.contentResolver.openInputStream(this)?.use { inputStream ->
        inputStream.readBytes()
    } ?: byteArrayOf()
}

private fun validateFileSize(context: Context, uri: Uri, maxBytes: Long, errorMessage: String) {
    val size = uri.fileSize(context)
    if (size != null && size > maxBytes) {
        throw IllegalArgumentException(errorMessage)
    }
}

private fun Uri.fileSize(context: Context): Long? {
    return context.contentResolver.openFileDescriptor(this, "r")?.use { descriptor ->
        descriptor.statSize.takeIf { it > 0 }
    }
}

private const val MAX_VIDEO_SIZE_BYTES = 50L * 1024L * 1024L
private const val MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L