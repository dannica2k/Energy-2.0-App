package com.example.energy20.api

import android.content.Context
import com.example.energy20.auth.AuthInterceptor
import com.example.energy20.data.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

/**
 * AuthApiService - Handles authentication API calls
 * 
 * Endpoints:
 * - POST /api/auth.php - Login with Google
 * - POST /api/add_device.php - Add device to account
 * - GET /api/get_devices.php - Get user's devices
 */
class AuthApiService(context: Context) {
    
    private val gson = Gson()
    private val baseUrl = "https://energy.webmenow.org/api"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context))
        .addInterceptor(loggingInterceptor)
        .build()
    
    /**
     * Authenticate user with Google ID token
     */
    suspend fun authenticate(idToken: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(mapOf("idToken" to idToken))
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/auth.php")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val authResponse = gson.fromJson(responseBody, AuthResponse::class.java)
                Result.success(authResponse)
            } else {
                val error = try {
                    gson.fromJson(responseBody, ApiError::class.java)
                } catch (e: Exception) {
                    ApiError("Authentication failed", "HTTP ${response.code}")
                }
                Result.failure(Exception(error.message ?: error.error))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Authentication error: ${e.message}"))
        }
    }
    
    /**
     * Add device to user's account
     */
    suspend fun addDevice(
        deviceId: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<AddDeviceResponse> = withContext(Dispatchers.IO) {
        try {
            val requestData = mutableMapOf<String, Any>("device_id" to deviceId)
            
            // Add optional location data if provided
            if (latitude != null && longitude != null) {
                requestData["latitude"] = latitude
                requestData["longitude"] = longitude
            }
            
            val json = gson.toJson(requestData)
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/add_device.php")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val addDeviceResponse = gson.fromJson(responseBody, AddDeviceResponse::class.java)
                Result.success(addDeviceResponse)
            } else {
                val error = try {
                    gson.fromJson(responseBody, ApiError::class.java)
                } catch (e: Exception) {
                    ApiError("Failed to add device", "HTTP ${response.code}")
                }
                Result.failure(Exception(error.message ?: error.error))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Add device error: ${e.message}"))
        }
    }
    
    /**
     * Get user's devices
     */
    suspend fun getDevices(): Result<GetDevicesResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/get_devices.php")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val devicesResponse = gson.fromJson(responseBody, GetDevicesResponse::class.java)
                Result.success(devicesResponse)
            } else {
                val error = try {
                    gson.fromJson(responseBody, ApiError::class.java)
                } catch (e: Exception) {
                    ApiError("Failed to get devices", "HTTP ${response.code}")
                }
                Result.failure(Exception(error.message ?: error.error))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Get devices error: ${e.message}"))
        }
    }
    
    /**
     * Remove device from user's account
     */
    suspend fun removeDevice(deviceId: String): Result<RemoveDeviceResponse> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(mapOf("device_id" to deviceId))
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/remove_device.php")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val removeDeviceResponse = gson.fromJson(responseBody, RemoveDeviceResponse::class.java)
                Result.success(removeDeviceResponse)
            } else {
                val error = try {
                    gson.fromJson(responseBody, ApiError::class.java)
                } catch (e: Exception) {
                    ApiError("Failed to remove device", "HTTP ${response.code}")
                }
                Result.failure(Exception(error.message ?: error.error))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Remove device error: ${e.message}"))
        }
    }
    
    /**
     * Set active device for user
     */
    suspend fun setActiveDevice(deviceId: String): Result<SetActiveDeviceResponse> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(mapOf("device_id" to deviceId))
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/set_active_device.php")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val setActiveResponse = gson.fromJson(responseBody, SetActiveDeviceResponse::class.java)
                Result.success(setActiveResponse)
            } else {
                val error = try {
                    gson.fromJson(responseBody, ApiError::class.java)
                } catch (e: Exception) {
                    ApiError("Failed to set active device", "HTTP ${response.code}")
                }
                Result.failure(Exception(error.message ?: error.error))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Set active device error: ${e.message}"))
        }
    }
    
    /**
     * Debug authentication - returns detailed diagnostic information
     * This helps diagnose authorization header issues
     */
    suspend fun debugAuth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/debug_auth.php")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                Result.success(responseBody)
            } else {
                Result.failure(Exception("Debug request failed: HTTP ${response.code}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Debug error: ${e.message}"))
        }
    }
}
