package com.example.energy20.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor - OkHttp interceptor for adding authentication
 * 
 * Automatically adds Bearer token to all API requests
 * Handles 401 Unauthorized responses by attempting silent token refresh
 * If refresh fails, triggers re-authentication
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
        const val ACTION_TOKEN_EXPIRED = "com.example.energy20.TOKEN_EXPIRED"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val authManager = AuthManager.getInstance(context)
        val originalRequest = chain.request()
        
        // Get current token
        var token = authManager.getAuthToken()
        
        // Build request with auth header if token exists
        val requestBuilder = originalRequest.newBuilder()
        
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d(TAG, "Added Authorization header to ${originalRequest.url}")
            Log.d(TAG, "Token length: ${token.length}")
        } else {
            Log.w(TAG, "No auth token available for ${originalRequest.url}")
        }
        
        // Execute request
        var response = chain.proceed(requestBuilder.build())
        
        Log.d(TAG, "Response code: ${response.code} for ${originalRequest.url}")
        
        // Handle 401 Unauthorized - attempt token refresh
        if (response.code == 401 && token != null) {
            Log.w(TAG, "Received 401 Unauthorized - attempting token refresh")
            
            // Close the original response
            response.close()
            
            // Attempt silent token refresh (blocking call in IO thread)
            val newToken = runBlocking {
                try {
                    authManager.refreshTokenSilently()
                } catch (e: Exception) {
                    Log.e(TAG, "Token refresh failed", e)
                    null
                }
            }
            
            if (newToken != null) {
                Log.d(TAG, "Token refresh successful - retrying request")
                
                // Retry the request with new token
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                
                response = chain.proceed(newRequest)
                Log.d(TAG, "Retry response code: ${response.code}")
            } else {
                Log.e(TAG, "Token refresh failed - user needs to re-authenticate")
                
                // Clear auth data
                authManager.clearAuthData()
                
                // Broadcast token expiration event
                val intent = Intent(ACTION_TOKEN_EXPIRED)
                context.sendBroadcast(intent)
                
                // Return the 401 response
                response = chain.proceed(requestBuilder.build())
            }
        }
        
        return response
    }
}
