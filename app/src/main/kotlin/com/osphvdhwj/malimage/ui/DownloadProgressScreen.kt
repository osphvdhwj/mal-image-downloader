/**
 * DownloadProgressScreen.kt
 *
 * Screen showing download progress and status for the MAL Image Downloader app.
 * Displays running downloads, completed downloads, and allows download management.
 *
 * Features:
 * - Real-time progress tracking for individual downloads
 * - Download queue management (pause, resume, cancel)
 * - Statistics and completion rates
 * - Error handling and retry options
 *
 * Author: osphvdhwj
 * Date: 2025-11-01
 */

package com.osphvdhwj.malimage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osphvdhwj.malimage.download.DownloadManager
import com.osphvdhwj.malimage.download.DownloadProgress
import com.osphvdhwj.malimage.download.DownloadStatus
import com.osphvdhwj.malimage.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadProgressScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val downloadManager = remember { DownloadManager(context) }
    
    var downloadStatus by remember { mutableStateOf<DownloadStatus?>(null) }
    var runningDownloads by remember { mutableStateOf<List<DownloadProgress>>(emptyList()) }
    
    // Refresh download status every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            downloadStatus = downloadManager.getDownloadStatus()
            runningDownloads = downloadManager.getRunningDownloadsProgress()
            delay(2000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Download Progress",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        downloadStatus?.let { status ->
            // Overall status card
            DownloadStatusCard(
                status = status,
                onCancelAll = { downloadManager.cancelAllDownloads() },
                onRetryFailed = { downloadManager.retryFailedDownloads() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Running downloads
            if (runningDownloads.isNotEmpty()) {
                Text(
                    "Active Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn {
                    items(runningDownloads) { download ->
                        DownloadProgressItem(
                            progress = download,
                            onCancel = { downloadManager.cancelDownloads(listOf(download.workId)) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else if (status.running == 0 && status.queued == 0) {
                // No active downloads
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (status.total > 0) "All downloads completed!" else "No downloads in progress",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (status.total > 0) {
                            Text(
                                "${status.succeeded} of ${status.total} downloads successful",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadStatusCard(
    status: DownloadStatus,
    onCancelAll: () -> Unit,
    onRetryFailed: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Overall Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem("Total", status.total.toString(), Icons.Default.List)
                StatusItem("Queued", status.queued.toString(), Icons.Default.Schedule)
                StatusItem("Running", status.running.toString(), Icons.Default.Download)
                StatusItem("Success", status.succeeded.toString(), Icons.Default.CheckCircle)
                if (status.failed > 0) {
                    StatusItem("Failed", status.failed.toString(), Icons.Default.Error)
                }
            }
            
            if (status.total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Success rate progress bar
                LinearProgressIndicator(
                    progress = status.successRate,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    "Success Rate: ${String.format("%.1f", status.successRate * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Action buttons
            if (status.running > 0 || status.queued > 0 || status.failed > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status.running > 0 || status.queued > 0) {
                        OutlinedButton(
                            onClick = onCancelAll,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel All")
                        }
                    }
                    
                    if (status.failed > 0) {
                        Button(onClick = onRetryFailed) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry Failed")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DownloadProgressItem(
    progress: DownloadProgress,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        progress.status,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        "${progress.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress.progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}