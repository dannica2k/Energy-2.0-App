package com.example.energy20.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
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
    
    fun downloadAndInstallUpdate(downloadUrl: String, onProgress: (Int) -> Unit = {}): Long {
        val fileName = "Energy20_update.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Energy20 Update")
            setDescription("Downloading latest version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        }
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        
        // Register receiver for download completion
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(context, downloadManager, id)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
        
        return downloadId
    }
    
    private fun installApk(context: Context, downloadManager: DownloadManager, downloadId: Long) {
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // For Android 7.0+, use FileProvider
                    val file = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "Energy20_update.apk")
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
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
