package com.example.energy20.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.energy20.api.AuthApiService
import com.example.energy20.auth.AuthManager
import com.example.energy20.data.UserDevice
import com.example.energy20.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var authManager: AuthManager
    private lateinit var authApiService: AuthApiService
    private lateinit var deviceAdapter: DeviceListAdapter
    
    companion object {
        private const val PREFS_NAME = "energy_settings"
        private const val KEY_OCCUPIED_THRESHOLD = "occupied_threshold"
        private const val KEY_LATITUDE = "weather_latitude"
        private const val KEY_LONGITUDE = "weather_longitude"
        private const val KEY_TEMP_UNIT = "temperature_unit"
        
        private const val DEFAULT_THRESHOLD = 1.0
        private const val DEFAULT_LATITUDE = 34.77 // Paphos, Cyprus
        private const val DEFAULT_LONGITUDE = 32.42
        private const val TEMP_UNIT_CELSIUS = "celsius"
        private const val TEMP_UNIT_FAHRENHEIT = "fahrenheit"
        
        fun getOccupiedThreshold(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_OCCUPIED_THRESHOLD, DEFAULT_THRESHOLD.toFloat()).toDouble()
        }
        
        fun setOccupiedThreshold(context: Context, threshold: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_OCCUPIED_THRESHOLD, threshold.toFloat()).apply()
        }
        
        fun getLatitude(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_LATITUDE, DEFAULT_LATITUDE.toFloat()).toDouble()
        }
        
        fun getLongitude(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_LONGITUDE, DEFAULT_LONGITUDE.toFloat()).toDouble()
        }
        
        fun setLocation(context: Context, latitude: Double, longitude: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat(KEY_LATITUDE, latitude.toFloat())
                .putFloat(KEY_LONGITUDE, longitude.toFloat())
                .apply()
        }
        
        fun isCelsius(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_TEMP_UNIT, TEMP_UNIT_CELSIUS) == TEMP_UNIT_CELSIUS
        }
        
        fun setTemperatureUnit(context: Context, isCelsius: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val unit = if (isCelsius) TEMP_UNIT_CELSIUS else TEMP_UNIT_FAHRENHEIT
            prefs.edit().putString(KEY_TEMP_UNIT, unit).apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        authManager = AuthManager.getInstance(requireContext())
        authApiService = AuthApiService(requireContext())
        
        setupUI()
        loadSettings()
        
        // Load devices from local storage
        loadDevices()
        
        return binding.root
    }
    
    private fun setupUI() {
        // Setup RecyclerView
        deviceAdapter = DeviceListAdapter { device ->
            showDeleteConfirmation(device)
        }
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
        
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
        
        binding.saveWeatherButton.setOnClickListener {
            saveWeatherSettings()
        }
        
        binding.addDeviceButton.setOnClickListener {
            addDevice()
        }
        
        // Manual refresh button - long press device ID input to refresh from server
        binding.deviceIdInput.setOnLongClickListener {
            refreshDevicesFromServer()
            Snackbar.make(binding.root, "Refreshing devices from server...", Snackbar.LENGTH_SHORT).show()
            true
        }
    }

    
    private fun loadSettings() {
        // Load occupancy threshold
        val threshold = getOccupiedThreshold(requireContext())
        binding.thresholdInput.setText(threshold.toString())
        binding.currentValueText.text = "Current: $threshold kWh"
        
        // Load weather settings
        val latitude = getLatitude(requireContext())
        val longitude = getLongitude(requireContext())
        binding.latitudeInput.setText(latitude.toString())
        binding.longitudeInput.setText(longitude.toString())
        
        // Load temperature unit
        val isCelsius = isCelsius(requireContext())
        if (isCelsius) {
            binding.celsiusRadio.isChecked = true
        } else {
            binding.fahrenheitRadio.isChecked = true
        }
    }
    
    private fun loadDevices() {
        val devices = authManager.getUserDevices()
        
        // LOGGING: Track device loading
        android.util.Log.d("SettingsFragment", "=== LOAD DEVICES ===")
        android.util.Log.d("SettingsFragment", "Device count: ${devices.size}")
        devices.forEachIndexed { index, device ->
            android.util.Log.d("SettingsFragment", "Device $index: ${device.deviceId} - ${device.deviceName}")
            android.util.Log.d("SettingsFragment", "  Added: ${device.addedAt}")
            android.util.Log.d("SettingsFragment", "  Timezone: ${device.timezoneId}")
            android.util.Log.d("SettingsFragment", "  Data count: ${device.dataCount}")
            android.util.Log.d("SettingsFragment", "  Last data: ${device.lastDataDate}")
        }
        
        deviceAdapter.submitList(devices)
        
        // Show/hide empty state
        if (devices.isEmpty()) {
            binding.devicesRecyclerView.visibility = View.GONE
            binding.emptyDevicesText.visibility = View.VISIBLE
        } else {
            binding.devicesRecyclerView.visibility = View.VISIBLE
            binding.emptyDevicesText.visibility = View.GONE
        }
    }
    
    private fun showDeleteConfirmation(device: UserDevice) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Device")
            .setMessage("Are you sure you want to remove device ${device.deviceId} (${device.deviceName}) from your account?\n\nThis will not delete the device's data, only remove it from your account.")
            .setPositiveButton("Remove") { _, _ ->
                deleteDevice(device)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteDevice(device: UserDevice) {
        lifecycleScope.launch {
            try {
                val result = authApiService.removeDevice(device.deviceId)
                
                result.onSuccess { response ->
                    // Update local storage with new device list
                    authManager.updateDevices(response.devices)
                    
                    // Reload devices list
                    loadDevices()
                    
                    Snackbar.make(
                        binding.root,
                        "Device ${device.deviceId} removed successfully",
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                }.onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        "Failed to remove device: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun addDevice() {
        val deviceId = binding.deviceIdInput.text.toString().trim().uppercase()
        
        if (deviceId.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a device ID", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        binding.addDeviceButton.isEnabled = false
        binding.addDeviceButton.text = "Adding..."
        
        lifecycleScope.launch {
            try {
                val result = authApiService.addDevice(deviceId)
                
                result.onSuccess { response ->
                    // Add device to local storage
                    authManager.addDevice(response.device)
                    
                    // Clear input
                    binding.deviceIdInput.text?.clear()
                    
                    // Reload devices list
                    loadDevices()
                    
                    // Show success message
                    binding.successText.text = "Device ${response.device.deviceId} added successfully!"
                    binding.successText.visibility = View.VISIBLE
                    binding.successText.postDelayed({
                        binding.successText.visibility = View.GONE
                    }, 3000)
                    
                    Snackbar.make(binding.root, "Device added! Refresh home screen to see data.", Snackbar.LENGTH_LONG).show()
                    
                }.onFailure { error ->
                    Snackbar.make(binding.root, "Failed to add device: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                // Reset button state
                binding.addDeviceButton.isEnabled = true
                binding.addDeviceButton.text = "Add"
            }
        }
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
    
    private fun saveWeatherSettings() {
        val latText = binding.latitudeInput.text.toString()
        val lonText = binding.longitudeInput.text.toString()
        
        if (latText.isEmpty() || lonText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter both latitude and longitude", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        try {
            val latitude = latText.toDouble()
            val longitude = lonText.toDouble()
            
            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                Snackbar.make(binding.root, "Latitude must be between -90 and 90", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            if (longitude < -180 || longitude > 180) {
                Snackbar.make(binding.root, "Longitude must be between -180 and 180", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            // Save location
            setLocation(requireContext(), latitude, longitude)
            
            // Save temperature unit
            val isCelsius = binding.celsiusRadio.isChecked
            setTemperatureUnit(requireContext(), isCelsius)
            
            // Show success message
            binding.successText.visibility = View.VISIBLE
            binding.successText.postDelayed({
                binding.successText.visibility = View.GONE
            }, 3000)
            
            Snackbar.make(binding.root, "Weather settings saved! Reload data to see changes.", Snackbar.LENGTH_LONG).show()
            
        } catch (e: NumberFormatException) {
            Snackbar.make(binding.root, "Please enter valid coordinates", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Refresh device list from server
     * Called when fragment becomes visible to ensure list is up-to-date
     */
    private fun refreshDevicesFromServer() {
        lifecycleScope.launch {
            try {
                val result = authApiService.getDevices()
                
                result.onSuccess { response ->
                    // Update local storage with server data
                    authManager.updateDevices(response.devices)
                    
                    // Reload UI
                    loadDevices()
                    
                    android.util.Log.d("SettingsFragment", "Devices refreshed from server: ${response.devices.size} devices")
                    
                }.onFailure { error ->
                    android.util.Log.e("SettingsFragment", "Failed to refresh devices: ${error.message}")
                    // Still show local devices even if refresh fails
                    loadDevices()
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Error refreshing devices", e)
                // Still show local devices even if refresh fails
                loadDevices()
            }
        }
    }
    
    /**
     * Debug authentication - calls debug endpoint and displays results
     * Triggered by long-pressing the device ID input field
     */
    private fun debugAuth() {
        Snackbar.make(binding.root, "Running auth diagnostics...", Snackbar.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val result = authApiService.debugAuth()
                
                result.onSuccess { debugJson ->
                    // Log the full response
                    android.util.Log.d("SettingsFragment", "=== AUTH DEBUG RESPONSE ===")
                    android.util.Log.d("SettingsFragment", debugJson)
                    android.util.Log.d("SettingsFragment", "=== END DEBUG RESPONSE ===")
                    
                    Snackbar.make(
                        binding.root, 
                        "Debug info logged! Check Logcat for 'SettingsFragment'", 
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                }.onFailure { error ->
                    android.util.Log.e("SettingsFragment", "Debug failed: ${error.message}")
                    Snackbar.make(binding.root, "Debug failed: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Debug error", e)
                Snackbar.make(binding.root, "Debug error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
