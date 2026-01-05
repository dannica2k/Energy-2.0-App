package com.example.energy20.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.energy20.data.User
import com.example.energy20.data.UserDevice
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

/**
 * AuthManager - Singleton for managing authentication state
 * 
 * Handles:
 * - Secure token storage using EncryptedSharedPreferences
 * - User session management
 * - Authentication state
 * - Silent token refresh
 */
class AuthManager private constructor(private val context: Context) {
    
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
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_USER_DEVICES = "user_devices"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        
        // Web Client ID for Google Sign-In
        private const val WEB_CLIENT_ID = "242021166104-he2lbj7avj8r6ujlhc79n7i6al087idv.apps.googleusercontent.com"
        
        // Refresh token if it expires in less than 5 minutes
        private const val REFRESH_THRESHOLD_MS = 5 * 60 * 1000L
        
        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
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
     * Checks if token is expired or about to expire
     */
    fun getAuthToken(): String? {
        val token = securePrefs.getString(KEY_AUTH_TOKEN, null)
        val expiry = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        
        if (token != null) {
            val now = System.currentTimeMillis()
            val timeUntilExpiry = expiry - now
            
            Log.d(TAG, "getAuthToken: Token exists (length: ${token.length})")
            Log.d(TAG, "Token expires in: ${timeUntilExpiry / 1000 / 60} minutes")
            
            // Token is expired
            if (expiry > 0 && now >= expiry) {
                Log.w(TAG, "Token is expired!")
                return null
            }
        } else {
            Log.d(TAG, "getAuthToken: No token")
        }
        
        return token
    }
    
    /**
     * Check if token needs refresh (expires in less than 5 minutes)
     */
    fun needsTokenRefresh(): Boolean {
        val expiry = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        if (expiry == 0L) return false
        
        val now = System.currentTimeMillis()
        val timeUntilExpiry = expiry - now
        
        return timeUntilExpiry < REFRESH_THRESHOLD_MS
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
        val devicesJson = securePrefs.getString(KEY_USER_DEVICES, null)
        
        Log.d(TAG, "=== GET USER DEVICES ===")
        Log.d(TAG, "Raw JSON from storage: $devicesJson")
        
        if (devicesJson == null) {
            Log.d(TAG, "No devices in storage")
            return emptyList()
        }
        
        return try {
            val devices = gson.fromJson(devicesJson, Array<UserDevice>::class.java).toList()
            Log.d(TAG, "Parsed ${devices.size} devices from storage")
            devices.forEachIndexed { index, device ->
                Log.d(TAG, "Device $index: ${device.deviceId} - ${device.deviceName}")
            }
            devices
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse devices from storage", e)
            emptyList()
        }
    }
    
    /**
     * Save authentication data
     * Google ID tokens typically expire in 1 hour (3600 seconds)
     */
    fun saveAuthData(token: String, user: User, devices: List<UserDevice>) {
        // Google ID tokens expire in 1 hour
        val expiryTime = System.currentTimeMillis() + (3600 * 1000L)
        
        Log.d(TAG, "=== SAVE AUTH DATA ===")
        Log.d(TAG, "User: ${user.email}")
        Log.d(TAG, "Devices to save: ${devices.size}")
        devices.forEachIndexed { index, device ->
            Log.d(TAG, "Device $index: ${device.deviceId} - ${device.deviceName}")
            Log.d(TAG, "  Timezone: ${device.timezoneId}, Active: ${device.isActive}")
            Log.d(TAG, "  Latitude: ${device.latitude}, Longitude: ${device.longitude}")
        }
        
        val devicesJson = gson.toJson(devices)
        Log.d(TAG, "Devices JSON: $devicesJson")
        
        securePrefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
            putString(KEY_USER_DATA, gson.toJson(user))
            putString(KEY_USER_DEVICES, devicesJson)
            putBoolean(KEY_IS_AUTHENTICATED, true)
            apply()
        }
        
        Log.d(TAG, "Auth data saved. Token expires at: ${java.util.Date(expiryTime)}")
    }
    
    /**
     * Update user's devices list
     */
    fun updateDevices(devices: List<UserDevice>) {
        Log.d(TAG, "=== UPDATE DEVICES ===")
        Log.d(TAG, "Updating with ${devices.size} devices")
        devices.forEachIndexed { index, device ->
            Log.d(TAG, "Device $index: ${device.deviceId} - ${device.deviceName}")
        }
        
        val devicesJson = gson.toJson(devices)
        Log.d(TAG, "Devices JSON: $devicesJson")
        
        securePrefs.edit().apply {
            putString(KEY_USER_DEVICES, devicesJson)
            apply()
        }
        
        Log.d(TAG, "Devices updated in storage")
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
     * Silently refresh the authentication token
     * Uses Google Sign-In to get a fresh ID token without user interaction
     * 
     * @return New token if successful, null if refresh failed
     */
    suspend fun refreshTokenSilently(): String? {
        return try {
            Log.d(TAG, "Attempting silent token refresh...")
            
            // Try to silently sign in to get fresh token
            val account = googleSignInClient.silentSignIn().await()
            val newToken = account?.idToken
            
            if (newToken != null) {
                Log.d(TAG, "Silent token refresh successful")
                
                // Update stored token with new expiry
                val expiryTime = System.currentTimeMillis() + (3600 * 1000L)
                securePrefs.edit().apply {
                    putString(KEY_AUTH_TOKEN, newToken)
                    putLong(KEY_TOKEN_EXPIRY, expiryTime)
                    apply()
                }
                
                Log.d(TAG, "New token saved. Expires at: ${java.util.Date(expiryTime)}")
                newToken
            } else {
                Log.w(TAG, "Silent refresh returned null token")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Silent token refresh failed", e)
            null
        }
    }
    
    /**
     * Clear all authentication data (logout)
     */
    fun clearAuthData() {
        securePrefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_TOKEN_EXPIRY)
            remove(KEY_USER_DATA)
            remove(KEY_USER_DEVICES)
            putBoolean(KEY_IS_AUTHENTICATED, false)
            apply()
        }
        
        // Sign out from Google
        googleSignInClient.signOut()
    }
    
    /**
     * Logout user
     */
    fun logout() {
        clearAuthData()
    }
}
