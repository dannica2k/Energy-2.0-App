package com.example.energy20.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.energy20.data.User
import com.example.energy20.data.UserDevice
import com.google.gson.Gson

/**
 * AuthManager - Singleton for managing authentication state
 * 
 * Handles:
 * - Secure token storage using EncryptedSharedPreferences
 * - User session management
 * - Authentication state
 */
class AuthManager private constructor(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "energy20_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val gson = Gson()
    
    companion object {
        private const val TAG = "AuthManager"
        
        @Volatile
        private var instance: AuthManager? = null
        
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_USER_DEVICES = "user_devices"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        
        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        val isAuth = securePrefs.getBoolean(KEY_IS_AUTHENTICATED, false) && 
               getAuthToken() != null
        Log.d(TAG, "isAuthenticated: $isAuth")
        return isAuth
    }
    
    /**
     * Get stored authentication token
     */
    fun getAuthToken(): String? {
        val token = securePrefs.getString(KEY_AUTH_TOKEN, null)
        Log.d(TAG, "getAuthToken: ${if (token != null) "Token exists (length: ${token.length})" else "No token"}")
        return token
    }
    
    /**
     * Get current user data
     */
    fun getCurrentUser(): User? {
        val userJson = securePrefs.getString(KEY_USER_DATA, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user's devices
     */
    fun getUserDevices(): List<UserDevice> {
        val devicesJson = securePrefs.getString(KEY_USER_DEVICES, null) ?: return emptyList()
        return try {
            gson.fromJson(devicesJson, Array<UserDevice>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Save authentication data
     */
    fun saveAuthData(token: String, user: User, devices: List<UserDevice>) {
        securePrefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_DATA, gson.toJson(user))
            putString(KEY_USER_DEVICES, gson.toJson(devices))
            putBoolean(KEY_IS_AUTHENTICATED, true)
            apply()
        }
    }
    
    /**
     * Update user's devices list
     */
    fun updateDevices(devices: List<UserDevice>) {
        securePrefs.edit().apply {
            putString(KEY_USER_DEVICES, gson.toJson(devices))
            apply()
        }
    }
    
    /**
     * Add a device to the user's list
     */
    fun addDevice(device: UserDevice) {
        val currentDevices = getUserDevices().toMutableList()
        currentDevices.add(0, device) // Add to beginning
        updateDevices(currentDevices)
    }
    
    /**
     * Clear all authentication data (logout)
     */
    fun clearAuthData() {
        securePrefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_DATA)
            remove(KEY_USER_DEVICES)
            putBoolean(KEY_IS_AUTHENTICATED, false)
            apply()
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        clearAuthData()
    }
}
