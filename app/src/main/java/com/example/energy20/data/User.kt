package com.example.energy20.data

import com.google.gson.annotations.SerializedName

/**
 * User data model
 * Represents an authenticated user in the Energy20 system
 */
data class User(
    val id: Int,
    val email: String,
    val name: String?,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String?,
    @SerializedName("google_id")
    val googleId: String
)

/**
 * Authentication response from backend
 */
data class AuthResponse(
    val success: Boolean,
    val user: User,
    val devices: List<UserDevice>,
    val token: String,
    @SerializedName("has_devices")
    val hasDevices: Boolean
)

/**
 * User device data model
 */
data class UserDevice(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("added_at")
    val addedAt: String,
    @SerializedName("timezone_id")
    val timezoneId: String? = null,
    @SerializedName("data_count")
    val dataCount: Int? = null,
    @SerializedName("last_data_date")
    val lastDataDate: String? = null
)

/**
 * Add device response
 */
data class AddDeviceResponse(
    val success: Boolean,
    val message: String,
    val device: UserDevice
)

/**
 * Get devices response
 */
data class GetDevicesResponse(
    val success: Boolean,
    val user: User,
    val devices: List<UserDevice>,
    @SerializedName("device_count")
    val deviceCount: Int
)

/**
 * Remove device response
 */
data class RemoveDeviceResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("device_id")
    val deviceId: String,
    val devices: List<UserDevice>,
    @SerializedName("device_count")
    val deviceCount: Int
)

/**
 * Error response from API
 */
data class ApiError(
    val error: String,
    val message: String? = null
)
