/**
 * ImageDownloadWorker.kt
 *
 * WorkManager worker for downloading images in the background with retry logic.
 * Handles downloading anime/manga images from URLs, creating proper folder structure,
 * and embedding metadata into downloaded images.
 *
 * Features:
 * - Background download with progress updates
 * - Automatic retry on network failures
 * - Creates organized folder structure
 * - Embeds MAL metadata into image files
 * - Respects battery and network constraints
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.download

import android.content.Context
import android.util.Log
import androidx.work.*
import com.osphvdhwj.malimage.metadata.MetadataEmbedder
import com.osphvdhwj.malimage.parser.AnimeEntry
import com.osphvdhwj.malimage.storage.FolderOrganizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ImageDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_IMAGE_URL = "image_url"
        const val KEY_ENTRY_JSON = "entry_json"
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val TAG = "ImageDownloadWorker"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val folderOrganizer = FolderOrganizer()
    private val metadataEmbedder = MetadataEmbedder()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val imageUrl = inputData.getString(KEY_IMAGE_URL) ?: return@withContext Result.failure()
        val entryJson = inputData.getString(KEY_ENTRY_JSON) ?: return@withContext Result.failure()
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()

        try {
            // Parse entry data
            val entry = parseEntryFromJson(entryJson)
            
            // Update progress
            setProgress(workDataOf("status" to "Downloading ${entry.title}", "progress" to 0))
            
            // Download image
            val imageData = downloadImage(imageUrl) ?: return@withContext Result.retry()
            
            setProgress(workDataOf("status" to "Organizing ${entry.title}", "progress" to 50))
            
            // Create organized folder structure
            val targetDir = folderOrganizer.organizeContent(entry)
            val imageFile = createImageFile(targetDir, entry, imageUrl)
            
            // Save image to file
            FileOutputStream(imageFile).use { output ->
                output.write(imageData)
            }
            
            setProgress(workDataOf("status" to "Adding metadata to ${entry.title}", "progress" to 80))
            
            // Embed metadata into the image
            metadataEmbedder.embedMetadata(imageFile, entry)
            
            setProgress(workDataOf("status" to "Completed ${entry.title}", "progress" to 100))
            
            Log.i(TAG, "Successfully downloaded and processed: ${entry.title}")
            Result.success(workDataOf("completed_file" to imageFile.absolutePath))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: $imageUrl", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }

    /**
     * Download image data from URL
     */
    private suspend fun downloadImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.w(TAG, "Failed to download image: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading image", e)
            null
        }
    }

    /**
     * Create properly named image file in target directory
     */
    private fun createImageFile(targetDir: File, entry: AnimeEntry, imageUrl: String): File {
        val extension = getImageExtension(imageUrl)
        val safeName = sanitizeFileName(entry.title ?: "unknown")
        val fileName = "${safeName}_${entry.id}.$extension"
        return File(targetDir, fileName)
    }

    /**
     * Get image extension from URL
     */
    private fun getImageExtension(url: String): String {
        return when {
            url.contains(".jpg", ignoreCase = true) -> "jpg"
            url.contains(".jpeg", ignoreCase = true) -> "jpg"
            url.contains(".png", ignoreCase = true) -> "png"
            url.contains(".webp", ignoreCase = true) -> "webp"
            else -> "jpg" // default
        }
    }

    /**
     * Sanitize filename for filesystem safety
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(50) // Limit length
    }

    /**
     * Parse AnimeEntry from JSON string
     */
    private fun parseEntryFromJson(json: String): AnimeEntry {
        val jsonObj = JSONObject(json)
        return AnimeEntry().apply {
            id = jsonObj.optInt("id")
            title = jsonObj.optString("title")
            imageUrl = jsonObj.optString("imageUrl")
            type = jsonObj.optInt("type")
            genres = jsonObj.optString("genres")
        }
    }
}