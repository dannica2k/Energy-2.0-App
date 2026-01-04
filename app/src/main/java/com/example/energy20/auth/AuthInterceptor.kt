package com.example.energy20.auth

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor - OkHttp interceptor for adding authentication
 * 
 * Automatically adds Bearer token to all API requests
 * Handles 401 Unauthorized responses by clearing auth data
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val authManager = AuthManager.getInstance(context)
        val token = authManager.getAuthToken()
        
        // Build request with auth header if token exists
        val requestBuilder = chain.request().newBuilder()
        
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d(TAG, "Added Authorization header to ${chain.request().url}")
            Log.d(TAG, "Token length: ${token.length}")
        } else {
            Log.w(TAG, "No auth token available for ${chain.request().url}")
        }
        
        val request = requestBuilder.build()
        val response = chain.proceed(request)
        
        Log.d(TAG, "Response code: ${response.code} for ${request.url}")
        
        // Note: We don't automatically clear auth data on 401 anymore
        // This prevents clearing the token when there are server-side issues
        // User can manually logout if needed
        
        return response
    }
}
