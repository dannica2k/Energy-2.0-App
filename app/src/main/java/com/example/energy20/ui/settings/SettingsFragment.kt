package com.example.energy20.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.energy20.BuildConfig
import com.example.energy20.databinding.FragmentSettingsBinding
import com.example.energy20.utils.UpdateCheckResult
import com.example.energy20.utils.UpdateManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var updateManager: UpdateManager
    
    companion object {
        private const val PREFS_NAME = "energy_settings"
        private const val KEY_OCCUPIED_THRESHOLD = "occupied_threshold"
        private const val DEFAULT_THRESHOLD = 1.0
        
        fun getOccupiedThreshold(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_OCCUPIED_THRESHOLD, DEFAULT_THRESHOLD.toFloat()).toDouble()
        }
        
        fun setOccupiedThreshold(context: Context, threshold: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_OCCUPIED_THRESHOLD, threshold.toFloat()).apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        updateManager = UpdateManager(requireContext())
        
        setupUI()
        loadSettings()
        updateVersionInfo()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
        
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
        
        updateManager.downloadAndInstallUpdate(downloadUrl)
        
        Snackbar.make(
            binding.root,
            "Downloading update... Check notifications for progress",
            Snackbar.LENGTH_LONG
        ).show()
    }
    
    private fun loadSettings() {
        val threshold = getOccupiedThreshold(requireContext())
        binding.thresholdInput.setText(threshold.toString())
        binding.currentValueText.text = "Current: $threshold kWh"
    }
    
    private fun saveSettings() {
        val thresholdText = binding.thresholdInput.text.toString()
        
        if (thresholdText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a threshold value", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        try {
            val threshold = thresholdText.toDouble()
            
            if (threshold < 0) {
                Snackbar.make(binding.root, "Threshold must be positive", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            setOccupiedThreshold(requireContext(), threshold)
            binding.currentValueText.text = "Current: $threshold kWh"
            
            // Show success message
            binding.successText.visibility = View.VISIBLE
            binding.successText.postDelayed({
                binding.successText.visibility = View.GONE
            }, 3000)
            
            Snackbar.make(binding.root, "Settings saved successfully!", Snackbar.LENGTH_SHORT).show()
            
        } catch (e: NumberFormatException) {
            Snackbar.make(binding.root, "Please enter a valid number", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
