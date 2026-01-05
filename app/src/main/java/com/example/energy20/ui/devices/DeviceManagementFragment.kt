package com.example.energy20.ui.devices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.energy20.api.AuthApiService
import com.example.energy20.auth.AuthManager
import com.example.energy20.data.UserDevice
import com.example.energy20.databinding.FragmentDeviceManagementBinding
import com.example.energy20.ui.qrscanner.QrScannerActivity
import com.example.energy20.ui.settings.DeviceListAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DeviceManagementFragment : Fragment() {

    private var _binding: FragmentDeviceManagementBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var authManager: AuthManager
    private lateinit var authApiService: AuthApiService
    private lateinit var deviceAdapter: DeviceListAdapter
    
    // Activity result launcher for QR scanner
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val deviceId = result.data?.getStringExtra(QrScannerActivity.EXTRA_DEVICE_ID)
            deviceId?.let {
                binding.deviceIdInput.setText(it)
                Snackbar.make(binding.root, "Device ID scanned: $it", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    // Permission launcher for camera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchQrScanner()
        } else {
            Snackbar.make(
                binding.root,
                "Camera permission is required to scan QR codes",
                Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                // User can manually enable in settings
            }.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceManagementBinding.inflate(inflater, container, false)
        
        authManager = AuthManager.getInstance(requireContext())
        authApiService = AuthApiService(requireContext())
        
        setupUI()
        loadDevices()
        
        return binding.root
    }
    
    private fun setupUI() {
        // Setup RecyclerView
        deviceAdapter = DeviceListAdapter(
            onDeleteClick = { device ->
                showDeleteConfirmation(device)
            },
            onSetActiveClick = { device, viewHolder ->
                setActiveDevice(device, viewHolder)
            }
        )
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
        
        binding.addDeviceButton.setOnClickListener {
            addDevice()
        }
        
        binding.scanQrButton.setOnClickListener {
            checkCameraPermissionAndScan()
        }
    }
    
    private fun loadDevices() {
        val devices = authManager.getUserDevices()
        
        // LOGGING: Track device loading
        android.util.Log.d("DeviceManagementFragment", "=== LOAD DEVICES ===")
        android.util.Log.d("DeviceManagementFragment", "Device count: ${devices.size}")
        devices.forEachIndexed { index, device ->
            android.util.Log.d("DeviceManagementFragment", "Device $index: ${device.deviceId} - ${device.deviceName}")
            android.util.Log.d("DeviceManagementFragment", "  Added: ${device.addedAt}")
            android.util.Log.d("DeviceManagementFragment", "  Active: ${device.isActive}")
            android.util.Log.d("DeviceManagementFragment", "  Timezone: ${device.timezoneId}")
            android.util.Log.d("DeviceManagementFragment", "  Latitude: ${device.latitude}")
            android.util.Log.d("DeviceManagementFragment", "  Longitude: ${device.longitude}")
        }
        
        deviceAdapter.submitList(devices)
        
        // Show/hide empty state
        if (devices.isEmpty()) {
            binding.devicesRecyclerView.visibility = View.GONE
            binding.emptyDevicesText.visibility = View.VISIBLE
        } else {
            binding.devicesRecyclerView.visibility = View.VISIBLE
            binding.emptyDevicesText.visibility = View.GONE
            
            // Calculate and set proper height for RecyclerView
            setRecyclerViewHeight(devices.size)
        }
    }
    
    /**
     * Dynamically set RecyclerView height based on number of items
     * This ensures all items are visible when RecyclerView is inside a ScrollView
     */
    private fun setRecyclerViewHeight(itemCount: Int) {
        binding.devicesRecyclerView.post {
            // Measure a single item to get its height
            val itemView = binding.devicesRecyclerView.layoutManager?.findViewByPosition(0)
            
            if (itemView != null) {
                // Use actual measured height of first item
                val itemHeight = itemView.height
                val totalHeight = itemHeight * itemCount
                
                android.util.Log.d("DeviceManagementFragment", "Setting RecyclerView height: itemHeight=$itemHeight, itemCount=$itemCount, totalHeight=$totalHeight")
                
                val params = binding.devicesRecyclerView.layoutParams
                params.height = totalHeight
                binding.devicesRecyclerView.layoutParams = params
            } else {
                // Fallback: estimate item height (card with padding + margins)
                // Each item_device.xml has: card with 16dp padding + 8dp bottom margin
                val density = resources.displayMetrics.density
                val estimatedItemHeight = (80 * density).toInt() // Approximate height in pixels
                val totalHeight = estimatedItemHeight * itemCount
                
                android.util.Log.d("DeviceManagementFragment", "Using estimated height: estimatedItemHeight=$estimatedItemHeight, itemCount=$itemCount, totalHeight=$totalHeight")
                
                val params = binding.devicesRecyclerView.layoutParams
                params.height = totalHeight
                binding.devicesRecyclerView.layoutParams = params
            }
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
    
    private fun setActiveDevice(device: UserDevice, viewHolder: DeviceListAdapter.DeviceViewHolder) {
        // Show loading state immediately
        viewHolder.showLoading()
        
        lifecycleScope.launch {
            try {
                val result = authApiService.setActiveDevice(device.deviceId)
                
                result.onSuccess { response ->
                    // Update local storage with new device list
                    authManager.updateDevices(response.devices)
                    
                    // Reload devices list to show updated active status
                    loadDevices()
                    
                    Snackbar.make(
                        binding.root,
                        "${device.deviceName} is now the active device",
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                }.onFailure { error ->
                    // Reset button on failure
                    viewHolder.resetButton()
                    
                    Snackbar.make(
                        binding.root,
                        "Failed to set active device: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // Reset button on error
                viewHolder.resetButton()
                
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
        val latText = binding.latitudeInput.text.toString().trim()
        val lonText = binding.longitudeInput.text.toString().trim()
        
        if (deviceId.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a device ID", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Parse and validate coordinates (optional fields)
        var latitude: Double? = null
        var longitude: Double? = null
        
        if (latText.isNotEmpty() || lonText.isNotEmpty()) {
            // If one is provided, both must be provided
            if (latText.isEmpty() || lonText.isEmpty()) {
                Snackbar.make(binding.root, "Please enter both latitude and longitude, or leave both empty", Snackbar.LENGTH_LONG).show()
                return
            }
            
            try {
                latitude = latText.toDouble()
                longitude = lonText.toDouble()
                
                // Validate coordinate ranges
                if (latitude < -90 || latitude > 90) {
                    Snackbar.make(binding.root, "Latitude must be between -90 and 90", Snackbar.LENGTH_LONG).show()
                    return
                }
                
                if (longitude < -180 || longitude > 180) {
                    Snackbar.make(binding.root, "Longitude must be between -180 and 180", Snackbar.LENGTH_LONG).show()
                    return
                }
            } catch (e: NumberFormatException) {
                Snackbar.make(binding.root, "Please enter valid numbers for coordinates", Snackbar.LENGTH_LONG).show()
                return
            }
        }
        
        // Show loading state
        binding.addDeviceButton.isEnabled = false
        binding.addDeviceButton.text = "Adding..."
        
        lifecycleScope.launch {
            try {
                val result = authApiService.addDevice(deviceId, latitude, longitude)
                
                result.onSuccess { response ->
                    // Add device to local storage
                    authManager.addDevice(response.device)
                    
                    // Clear inputs
                    binding.deviceIdInput.text?.clear()
                    binding.latitudeInput.text?.clear()
                    binding.longitudeInput.text?.clear()
                    
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
                binding.addDeviceButton.text = "Add Device"
            }
        }
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                launchQrScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale and request permission
                AlertDialog.Builder(requireContext())
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera access is needed to scan QR codes on your energy monitoring devices.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                // Request permission directly
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun launchQrScanner() {
        val intent = Intent(requireContext(), QrScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
