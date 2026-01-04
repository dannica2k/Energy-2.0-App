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
 * RecyclerView adapter for displaying user devices with delete functionality
 */
class DeviceListAdapter(
    private val onDeleteClick: (UserDevice) -> Unit
) : ListAdapter<UserDevice, DeviceListAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding,
        private val onDeleteClick: (UserDevice) -> Unit
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
            
            // Set delete button click listener
            binding.deleteButton.setOnClickListener {
                onDeleteClick(device)
            }
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
