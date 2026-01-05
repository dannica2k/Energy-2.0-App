package com.example.energy20.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.energy20.data.UserDevice
import com.example.energy20.databinding.ItemDeviceBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RecyclerView adapter for displaying user devices with delete and set active functionality
 */
class DeviceListAdapter(
    private val onDeleteClick: (UserDevice) -> Unit,
    private val onSetActiveClick: (UserDevice, DeviceViewHolder) -> Unit
) : ListAdapter<UserDevice, DeviceListAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onDeleteClick, onSetActiveClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding,
        private val onDeleteClick: (UserDevice) -> Unit,
        private val onSetActiveClick: (UserDevice, DeviceViewHolder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: UserDevice) {
            binding.deviceIdText.text = device.deviceId
            binding.deviceNameText.text = device.deviceName
            
            // Format the added date
            val addedDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(device.addedAt)
                date?.let { outputFormat.format(it) } ?: device.addedAt
            } catch (e: Exception) {
                device.addedAt
            }
            
            binding.addedDateText.text = "Added: $addedDate"
            
            // Show/hide active status badge
            if (device.isActive) {
                binding.activeStatusText.visibility = android.view.View.VISIBLE
                binding.setActiveButton.visibility = android.view.View.GONE
            } else {
                binding.activeStatusText.visibility = android.view.View.GONE
                binding.setActiveButton.visibility = android.view.View.VISIBLE
            }
            
            // Set active button click listener
            binding.setActiveButton.setOnClickListener {
                onSetActiveClick(device, this)
            }
            
            // Set delete button click listener
            binding.deleteButton.setOnClickListener {
                onDeleteClick(device)
            }
        }
        
        /**
         * Show loading state on the "Make Active" button
         */
        fun showLoading() {
            binding.setActiveButton.isEnabled = false
            binding.setActiveButton.text = "Activating..."
            binding.setActiveButton.alpha = 0.6f
        }
        
        /**
         * Reset button to normal state
         */
        fun resetButton() {
            binding.setActiveButton.isEnabled = true
            binding.setActiveButton.text = "MAKE ACTIVE"
            binding.setActiveButton.alpha = 1.0f
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<UserDevice>() {
        override fun areItemsTheSame(oldItem: UserDevice, newItem: UserDevice): Boolean {
            return oldItem.deviceId == newItem.deviceId
        }

        override fun areContentsTheSame(oldItem: UserDevice, newItem: UserDevice): Boolean {
            return oldItem == newItem
        }
    }
}
