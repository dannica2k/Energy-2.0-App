package com.example.energy20.ui.about

import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.energy20.BuildConfig
import com.example.energy20.databinding.FragmentAboutBinding
import com.example.energy20.utils.UpdateCheckResult
import com.example.energy20.utils.UpdateManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var updateManager: UpdateManager
    private var currentDownloadId: Long = -1
    private var isPolling = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        
        updateManager = UpdateManager(requireContext())
        
        setupUI()
        updateVersionInfo()
        
        return binding.root
    }
    
    private fun setupUI() {
        // Set app info
        binding.appVersionText.text = "Version ${BuildConfig.VERSION_NAME}"
        
        // Update checker button
        binding.checkUpdateButton.setOnClickListener {
            checkForUpdates()
        }
    }
    
    private fun updateVersionInfo() {
        binding.currentVersionText.text = "Current Version: ${BuildConfig.VERSION_NAME}"
        
        val lastCheck = updateManager.getLastCheckTime()
        if (lastCheck > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
            binding.lastCheckText.text = "Last checked: ${dateFormat.format(Date(lastCheck))}"
        } else {
            binding.lastCheckText.text = "Never checked for updates"
        }
    }
    
    private fun checkForUpdates() {
        binding.checkUpdateButton.isEnabled = false
        binding.updateProgressBar.visibility = View.VISIBLE
        binding.updateStatusText.text = "Checking for updates..."
        binding.updateStatusText.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            when (val result = updateManager.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatusText.visibility = View.GONE
                    binding.checkUpdateButton.isEnabled = true
                    updateVersionInfo()
                    showUpdateDialog(result)
                }
                is UpdateCheckResult.NoUpdate -> {
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatusText.text = "You're up to date!"
                    binding.checkUpdateButton.isEnabled = true
                    updateVersionInfo()
                    
                    binding.updateStatusText.postDelayed({
                        binding.updateStatusText.visibility = View.GONE
                    }, 3000)
                }
                is UpdateCheckResult.Error -> {
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatusText.text = "Error: ${result.message}"
                    binding.checkUpdateButton.isEnabled = true
                    
                    binding.updateStatusText.postDelayed({
                        binding.updateStatusText.visibility = View.GONE
                    }, 5000)
                }
            }
        }
    }
    
    private fun showUpdateDialog(result: UpdateCheckResult.UpdateAvailable) {
        val release = result.release
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Update Available")
            .setMessage("""
                New version ${release.tagName} is available!
                
                Current version: ${BuildConfig.VERSION_NAME}
                
                ${release.body ?: "No release notes available."}
                
                Would you like to download and install this update?
            """.trimIndent())
            .setPositiveButton("Download") { _, _ ->
                downloadUpdate(result.downloadUrl, release.tagName)
            }
            .setNegativeButton("Later", null)
            .setNeutralButton("Skip This Version") { _, _ ->
                updateManager.ignoreVersion(release.tagName)
                Snackbar.make(binding.root, "Version ${release.tagName} will be skipped", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun downloadUpdate(downloadUrl: String, version: String) {
        binding.updateStatusText.text = "Downloading update..."
        binding.updateStatusText.visibility = View.VISIBLE
        binding.updateProgressBar.visibility = View.VISIBLE
        binding.checkUpdateButton.isEnabled = false
        
        currentDownloadId = updateManager.downloadAndInstallUpdate(downloadUrl) { status ->
            requireActivity().runOnUiThread {
                handleDownloadStatus(status)
            }
        }
        
        // Start polling the download status
        startPollingDownloadStatus()
        
        Snackbar.make(
            binding.root,
            "Downloading update... Check notifications for progress",
            Snackbar.LENGTH_LONG
        ).show()
    }
    
    private fun startPollingDownloadStatus() {
        if (isPolling) return
        isPolling = true
        
        lifecycleScope.launch {
            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            while (isPolling && currentDownloadId != -1L) {
                val status = updateManager.getDownloadStatus(downloadManager, currentDownloadId)
                
                requireActivity().runOnUiThread {
                    handleDownloadStatus(status)
                }
                
                // Stop polling if download is complete (success or failure)
                when (status) {
                    is com.example.energy20.utils.DownloadStatus.Success,
                    is com.example.energy20.utils.DownloadStatus.Failed,
                    is com.example.energy20.utils.DownloadStatus.Unknown -> {
                        isPolling = false
                    }
                    else -> {
                        // Continue polling
                    }
                }
                
                delay(1000) // Poll every second
            }
        }
    }
    
    private fun handleDownloadStatus(status: com.example.energy20.utils.DownloadStatus) {
        when (status) {
            is com.example.energy20.utils.DownloadStatus.Success -> {
                isPolling = false
                binding.updateProgressBar.visibility = View.GONE
                binding.updateStatusText.text = "Download complete! Installing..."
                binding.updateStatusText.postDelayed({
                    binding.updateStatusText.visibility = View.GONE
                    binding.checkUpdateButton.isEnabled = true
                }, 3000)
            }
            is com.example.energy20.utils.DownloadStatus.Failed -> {
                isPolling = false
                binding.updateProgressBar.visibility = View.GONE
                binding.updateStatusText.text = "Download failed: ${status.reason}"
                binding.checkUpdateButton.isEnabled = true
                
                Snackbar.make(
                    binding.root,
                    "Download failed: ${status.reason}",
                    Snackbar.LENGTH_LONG
                ).show()
                
                binding.updateStatusText.postDelayed({
                    binding.updateStatusText.visibility = View.GONE
                }, 5000)
            }
            is com.example.energy20.utils.DownloadStatus.Running -> {
                binding.updateStatusText.text = "Downloading: ${status.progress}% (${status.bytesDownloaded / 1024}KB / ${status.bytesTotal / 1024}KB)"
            }
            is com.example.energy20.utils.DownloadStatus.Pending -> {
                binding.updateStatusText.text = "Download pending..."
            }
            is com.example.energy20.utils.DownloadStatus.Paused -> {
                binding.updateStatusText.text = "Download paused: ${status.reason}"
            }
            is com.example.energy20.utils.DownloadStatus.Unknown -> {
                isPolling = false
                binding.updateProgressBar.visibility = View.GONE
                binding.updateStatusText.text = "Download status unknown: ${status.message}"
                binding.checkUpdateButton.isEnabled = true
                
                binding.updateStatusText.postDelayed({
                    binding.updateStatusText.visibility = View.GONE
                }, 5000)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPolling = false
        _binding = null
    }
}
