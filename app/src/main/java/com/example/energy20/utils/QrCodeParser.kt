package com.example.energy20.utils

import org.json.JSONException
import org.json.JSONObject

/**
 * Data class representing scanned device information from QR code
 */
data class ScannedDeviceData(
    val deviceId: String
)

/**
 * Utility object for parsing and validating QR codes containing device information
 */
object QrCodeParser {
    
    private const val EXPECTED_TYPE = "energy.webmenow.org"
    private const val DEVICE_ID_PATTERN = "[A-F0-9]{12}" // 12 hexadecimal characters
    
    /**
     * Parse QR code content and extract device information
     * 
     * Supports two formats:
     * 1. JSON: {"device_id":"D072F19EF0C8","type":"energy.webmenow.org"}
     * 2. Plain text: D072F19EF0C8 (backward compatibility)
     * 
     * @param qrContent The raw content from the scanned QR code
     * @return Result containing ScannedDeviceData on success, or error on failure
     */
    fun parse(qrContent: String): Result<ScannedDeviceData> {
        if (qrContent.isBlank()) {
            return Result.failure(Exception("QR code is empty"))
        }
        
        // Try JSON format first (preferred)
        try {
            val json = JSONObject(qrContent)
            
            // Validate type field
            val type = json.optString("type", "")
            if (type != EXPECTED_TYPE) {
                return Result.failure(Exception("Invalid QR code: Expected type '$EXPECTED_TYPE', got '$type'"))
            }
            
            // Extract device ID
            val deviceId = json.optString("device_id", "")
            if (deviceId.isEmpty()) {
                return Result.failure(Exception("QR code missing device_id field"))
            }
            
            // Validate device ID format
            val normalizedId = deviceId.trim().uppercase()
            if (!normalizedId.matches(Regex(DEVICE_ID_PATTERN))) {
                return Result.failure(Exception("Invalid device ID format: Expected 12 hexadecimal characters"))
            }
            
            return Result.success(ScannedDeviceData(deviceId = normalizedId))
            
        } catch (e: JSONException) {
            // Not JSON - try plain text format (backward compatibility)
            val deviceId = qrContent.trim().uppercase()
            
            if (deviceId.matches(Regex(DEVICE_ID_PATTERN))) {
                return Result.success(ScannedDeviceData(deviceId = deviceId))
            }
            
            return Result.failure(Exception("Invalid QR code format: Must be JSON with type '$EXPECTED_TYPE' or plain 12-character hex device ID"))
        }
    }
    
    /**
     * Validate if a string matches the expected device ID pattern
     */
    fun isValidDeviceId(deviceId: String): Boolean {
        return deviceId.uppercase().matches(Regex(DEVICE_ID_PATTERN))
    }
}
