/**
 * DownloadManager.kt
 *
 * Manages the download queue and orchestrates image downloading using WorkManager.
 * Handles batch enqueueing of downloads from MAL XML entries and provides status tracking.
 *
 * Features:
 * - Batch download enqueueing from MAL entries
 * - Download queue management and status tracking
 * - Network and battery constraints for downloads
 * - Progress monitoring and completion callbacks
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.download

import android.content.Context
import androidx.work.*
import com.osphvdhwj.malimage.parser.AnimeEntry
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        const val DOWNLOAD_WORK_TAG = "mal_image_download"
        private const val UNIQUE_WORK_PREFIX = "download_"
    }

    /**
     * Enqueue downloads for a list of MAL entries
     * 
     * @param entries List of AnimeEntry objects to download
     * @param onlyWiFi Whether to restrict downloads to WiFi only
     * @param requireCharging Whether to require device to be charging
     * @return List of work IDs for tracking
     */
    fun enqueueDownloads(
        entries: List<AnimeEntry>,
        onlyWiFi: Boolean = true,
        requireCharging: Boolean = false
    ): List<UUID> {
        
        val workIds = mutableListOf<UUID>()
        
        entries.forEach { entry ->
            if (entry.imageUrl != null && entry.imageUrl!!.isNotEmpty()) {
                val workId = enqueueDownload(entry, onlyWiFi, requireCharging)
                workIds.add(workId)
            }
        }
        
        return workIds
    }

    /**
     * Enqueue download for a single MAL entry
     */
    private fun enqueueDownload(
        entry: AnimeEntry,
        onlyWiFi: Boolean,
        requireCharging: Boolean
    ): UUID {
        
        // Convert entry to JSON for passing to worker
        val entryJson = JSONObject().apply {
            put("id", entry.id)
            put("title", entry.title)
            put("imageUrl", entry.imageUrl)
            put("type", entry.type)
            put("genres", entry.genres)
        }.toString()
        
        val downloadId = "${UNIQUE_WORK_PREFIX}${entry.id}_${System.currentTimeMillis()}"
        
        // Set up constraints
        val constraints = Constraints.Builder().apply {
            if (onlyWiFi) {
                setRequiredNetworkType(NetworkType.UNMETERED)
            } else {
                setRequiredNetworkType(NetworkType.CONNECTED)
            }
            if (requireCharging) {
                setRequiresCharging(true)
            }
            setRequiresBatteryNotLow(true)
        }.build()
        
        // Create work request
        val downloadRequest = OneTimeWorkRequestBuilder<ImageDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    ImageDownloadWorker.KEY_IMAGE_URL to entry.imageUrl,
                    ImageDownloadWorker.KEY_ENTRY_JSON to entryJson,
                    ImageDownloadWorker.KEY_DOWNLOAD_ID to downloadId
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(DOWNLOAD_WORK_TAG)
            .addTag(downloadId)
            .build()
        
        // Enqueue the work
        workManager.enqueueUniqueWork(
            downloadId,
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
        
        return downloadRequest.id
    }

    /**
     * Get status of all download works
     */
    fun getDownloadStatus(): DownloadStatus {
        val workInfos = workManager.getWorkInfosByTag(DOWNLOAD_WORK_TAG).get()
        
        var queued = 0
        var running = 0
        var succeeded = 0
        var failed = 0
        var cancelled = 0
        
        workInfos.forEach { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> queued++
                WorkInfo.State.RUNNING -> running++
                WorkInfo.State.SUCCEEDED -> succeeded++
                WorkInfo.State.FAILED -> failed++
                WorkInfo.State.CANCELLED -> cancelled++
                WorkInfo.State.BLOCKED -> queued++ // Treat blocked as queued
            }
        }
        
        return DownloadStatus(
            queued = queued,
            running = running,
            succeeded = succeeded,
            failed = failed,
            cancelled = cancelled,
            total = workInfos.size
        )
    }

    /**
     * Cancel all pending downloads
     */
    fun cancelAllDownloads() {
        workManager.cancelAllWorkByTag(DOWNLOAD_WORK_TAG)
    }

    /**
     * Cancel downloads by work IDs
     */
    fun cancelDownloads(workIds: List<UUID>) {
        workIds.forEach { workId ->
            workManager.cancelWorkById(workId)
        }
    }

    /**
     * Get progress of running downloads
     */
    fun getRunningDownloadsProgress(): List<DownloadProgress> {
        val workInfos = workManager.getWorkInfosByTag(DOWNLOAD_WORK_TAG).get()
        
        return workInfos
            .filter { it.state == WorkInfo.State.RUNNING }
            .map { workInfo ->
                val progress = workInfo.progress
                DownloadProgress(
                    workId = workInfo.id,
                    status = progress.getString("status") ?: "Downloading...",
                    progress = progress.getInt("progress", 0)
                )
            }
    }

    /**
     * Retry failed downloads
     */
    fun retryFailedDownloads() {
        val workInfos = workManager.getWorkInfosByTag(DOWNLOAD_WORK_TAG).get()
        
        workInfos
            .filter { it.state == WorkInfo.State.FAILED }
            .forEach { workInfo ->
                // Re-enqueue the failed work
                val inputData = workInfo.outputData
                val retryRequest = OneTimeWorkRequestBuilder<ImageDownloadWorker>()
                    .setInputData(inputData)
                    .addTag(DOWNLOAD_WORK_TAG)
                    .build()
                
                workManager.enqueue(retryRequest)
            }
    }
}

/**
 * Data class representing download status summary
 */
data class DownloadStatus(
    val queued: Int,
    val running: Int,
    val succeeded: Int,
    val failed: Int,
    val cancelled: Int,
    val total: Int
) {
    val isCompleted: Boolean
        get() = queued == 0 && running == 0
    
    val successRate: Float
        get() = if (total > 0) succeeded.toFloat() / total else 0f
}

/**
 * Data class representing individual download progress
 */
data class DownloadProgress(
    val workId: UUID,
    val status: String,
    val progress: Int
)