package com.example.energy20.data

/**
 * Represents energy data for a single device/circuit combination
 * Matches the JSON structure from daily_consumption.php
 */
data class DeviceInfo(
    val name: String,
    val data: Map<String, Double>  // Date (YYYY-MM-DD) to kWh
)

/**
 * Type alias for the top-level data structure
 * Map key format: "device_id_circuit_id" (e.g., "device_1_1")
 */
typealias DeviceEnergyData = Map<String, DeviceInfo>
