package com.example.energy20.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.energy20.BuildConfig
import com.example.energy20.data.GitHubRelease
import com.example.energy20.network.GitHubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

class UpdateManager(private val context: Context) {
    
    private val gitHubApi = GitHubApiService(OkHttpClient())
    private var downloadId: Long = -1
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_IGNORED_VERSION = "ignored_version"
        
        fun compareVersions(current: String, latest: String): Int {
            // Remove 'v' prefix if present
            val currentClean = current.removePrefix("v")
            val latestClean = latest.removePrefix("v")
            
            val currentParts = currentClean.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latestClean.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(currentParts.size, latestParts.size)
            
            for (i in 0 until maxLength) {
                val currentPart = currentParts.getOrNull(i) ?: 0
                val latestPart = latestParts.getOrNull(i) ?: 0
                
                when {
                    latestPart > currentPart -> return 1  // Update available
                    latestPart < currentPart -> return -1 // Current is newer
                }
            }
            
            return 0 // Same version
        }
    }
    
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val result = gitHubApi.getLatestRelease()
            
            if (result.isFailure) {
                return@withContext UpdateCheckResult.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
            
            val release = result.getOrNull() ?: return@withContext UpdateCheckResult.Error("No release found")
            
            // Find APK asset
            val apkAsset = release.assets.find { 
                it.name.endsWith(".apk", ignoreCase = true) 
            }
            
            if (apkAsset == null) {
                return@withContext UpdateCheckResult.Error("No APK found in release")
            }
            
            val currentVersion = BuildConfig.VERSION_NAME
            val latestVersion = release.tagName
            
            val comparison = compareVersions(currentVersion, latestVersion)
            
            when {
                comparison > 0 -> {
                    // Update available
                    saveLastCheckTime()
                    UpdateCheckResult.UpdateAvailable(release, apkAsset.downloadUrl)
                }
                comparison < 0 -> {
                    // Current version is newer (shouldn't happen in production)
                    UpdateCheckResult.NoUpdate
                }
                else -> {
                    // Same version
                    saveLastCheckTime()
                    UpdateCheckResult.NoUpdate
                }
            }
            
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }
    
    fun downloadAndInstallUpdate(downloadUrl: String, onStatusChange: (DownloadStatus) -> Unit = {}): Long {
        val fileName = "Energy20_update.apk"
        
        Log.d(TAG, "Starting download from: $downloadUrl")
        
        // Use app-specific storage (no permissions required)
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        Log.d(TAG, "Download directory: ${downloadDir?.absolutePath}")
        
        // Ensure directory exists
        if (downloadDir != null && !downloadDir.exists()) {
            val created = downloadDir.mkdirs()
            Log.d(TAG, "Created download directory: $created")
        }
        
        val destination = File(downloadDir, fileName)
        
        // Delete old file if exists
        if (destination.exists()) {
            val deleted = destination.delete()
            Log.d(TAG, "Deleted old APK file: $deleted")
        }
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Energy20 Update")
            setDescription("Downloading latest version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            // Use setDestinationInExternalFilesDir instead of setDestinationUri
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Download enqueued with ID: $downloadId")
        
        // Register receiver for download completion
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                Log.d(TAG, "Download complete broadcast received for ID: $id")
                
                if (id == downloadId) {
                    val status = getDownloadStatus(downloadManager, id)
                    Log.d(TAG, "Download status: $status")
                    
                    when (status) {
                        is DownloadStatus.Success -> {
                            onStatusChange(status)
                            installApk(context, downloadManager, id)
                        }
                        is DownloadStatus.Failed -> {
                            onStatusChange(status)
                            Log.e(TAG, "Download failed: ${status.reason}")
                        }
                        else -> {
                            onStatusChange(status)
                            Log.w(TAG, "Unexpected download status: $status")
                        }
                    }
                    
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
        
        return downloadId
    }
    
    fun getDownloadStatus(downloadManager: DownloadManager, downloadId: Long): DownloadStatus {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)
        
        return try {
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                
                val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                val bytesTotal = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else 0L
                val localUri = if (uriIndex >= 0) cursor.getString(uriIndex) else null
                
                Log.d(TAG, "Status code: $status, Reason: $reason, Downloaded: $bytesDownloaded/$bytesTotal, URI: $localUri")
                
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        DownloadStatus.Success(localUri ?: "")
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val failureReason = getFailureReason(reason)
                        DownloadStatus.Failed(failureReason)
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val progress = if (bytesTotal > 0) {
                            ((bytesDownloaded * 100) / bytesTotal).toInt()
                        } else 0
                        DownloadStatus.Running(progress, bytesDownloaded, bytesTotal)
                    }
                    DownloadManager.STATUS_PENDING -> {
                        DownloadStatus.Pending
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        val pauseReason = getPauseReason(reason)
                        DownloadStatus.Paused(pauseReason)
                    }
                    else -> {
                        DownloadStatus.Unknown("Unknown status: $status")
                    }
                }
            } else {
                Log.e(TAG, "Cursor is null or empty")
                DownloadStatus.Unknown("Download not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying download status", e)
            DownloadStatus.Unknown("Error: ${e.message}")
        } finally {
            cursor?.close()
        }
    }
    
    private fun getFailureReason(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Error code: $reason"
        }
    }
    
    private fun getPauseReason(reason: Int): String {
        return when (reason) {
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for WiFi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            DownloadManager.PAUSED_UNKNOWN -> "Paused (unknown reason)"
            else -> "Paused (code: $reason)"
        }
    }
    
    private fun installApk(context: Context, downloadManager: DownloadManager, downloadId: Long) {
        // Use app-specific storage location
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Energy20_update.apk")
        
        Log.d(TAG, "Attempting to install APK from: ${file.absolutePath}")
        Log.d(TAG, "File exists: ${file.exists()}, Size: ${file.length()} bytes")
        
        if (file.exists() && file.length() > 0) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // For Android 7.0+, use FileProvider
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        Log.d(TAG, "FileProvider URI: $apkUri")
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(intent)
                Log.d(TAG, "Install intent launched successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error launching install intent", e)
            }
        } else {
            Log.e(TAG, "APK file not found or empty")
        }
    }
    
    fun ignoreVersion(version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_IGNORED_VERSION, version).apply()
    }
    
    fun isVersionIgnored(version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IGNORED_VERSION, null) == version
    }
    
    private fun saveLastCheckTime() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }
    
    fun getLastCheckTime(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CHECK, 0)
    }
}

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubRelease, val downloadUrl: String) : UpdateCheckResult()
    object NoUpdate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadStatus {
    data class Success(val localUri: String) : DownloadStatus()
    data class Failed(val reason: String) : DownloadStatus()
    data class Running(val progress: Int, val bytesDownloaded: Long, val bytesTotal: Long) : DownloadStatus()
    object Pending : DownloadStatus()
    data class Paused(val reason: String) : DownloadStatus()
    data class Unknown(val message: String) : DownloadStatus()
}
