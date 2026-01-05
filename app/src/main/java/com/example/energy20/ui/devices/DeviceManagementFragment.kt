package com.example.energy20.ui.devices

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
import com.example.energy20.databinding.FragmentDeviceManagementBinding
import com.example.energy20.ui.settings.DeviceListAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DeviceManagementFragment : Fragment() {

    private var _binding: FragmentDeviceManagementBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var authManager: AuthManager
    private lateinit var authApiService: AuthApiService
    private lateinit var deviceAdapter: DeviceListAdapter

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
