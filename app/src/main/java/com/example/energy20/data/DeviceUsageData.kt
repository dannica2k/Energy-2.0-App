package com.example.energy20.data

import com.google.gson.annotations.SerializedName

/**
 * Response from get_usage_data.php API
 */
data class DeviceUsageResponse(
    @SerializedName("device_settings")
    val deviceSettings: DeviceSettings,
    
    @SerializedName("energy_summary")
    val energySummary: EnergySummary,
    
    @SerializedName("devices")
    val devices: List<Device>
)

data class DeviceSettings(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("timezone_id")
    val timezoneId: String,
    
    @SerializedName("cost_per_kwh")
    val costPerKwh: Double,
    
    @SerializedName("daily_kwh_allowance")
    val dailyKwhAllowance: Double,
    
    @SerializedName("new_firmware_version")
    val newFirmwareVersion: String,
    
    @SerializedName("firmware_update_required")
    val firmwareUpdateRequired: Boolean
)

data class EnergySummary(
    @SerializedName("start_date")
    val startDate: String,
    
    @SerializedName("today_kwh")
    val todayKwh: Double,
    
    @SerializedName("total_kwh")
    val totalKwh: Double,
    
    @SerializedName("days_since_start")
    val daysSinceStart: Int,
    
    @SerializedName("cumulative_allowance")
    val cumulativeAllowance: Double,
    
    @SerializedName("billable_kwh")
    val billableKwh: Double,
    
    @SerializedName("cost")
    val cost: Double
)

data class Device(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("sensor_id")
    val sensorId: Int,
    
    @SerializedName("device_name")
    val deviceName: String,
    
    @SerializedName("burden")
    val burden: Int
)
