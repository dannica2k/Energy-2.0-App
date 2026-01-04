package com.example.energy20.network

import android.content.Context
import com.example.energy20.auth.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Service for fetching energy data from the authenticated API
 */
class EnergyApiService(context: Context) {
    
    private val client: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val baseUrl = "https://energy.webmenow.org/api"
    
    /**
     * Fetches device usage data including settings and energy summary
     * Uses the new authenticated API endpoint
     * 
     * @param deviceId The device ID to fetch data for
     * @return JSON string with device usage data
     * @throws Exception if network request fails
     */
    suspend fun fetchDeviceUsageData(
        deviceId: String
    ): String {
        val url = "$baseUrl/get_device_usage.php?device_id=$deviceId"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("HTTP error: ${response.code}")
        }
        
        return response.body?.string() ?: throw Exception("Empty response body")
    }
    
    /**
     * Fetches daily energy consumption data
     * Uses the new authenticated API endpoint (returns pure JSON, not HTML)
     * 
     * @param startDate Date in YYYY-MM-DD format
     * @param endDate Date in YYYY-MM-DD format
     * @param deviceId The device ID to fetch data for
     * @return JSON string with energy data
     * @throws Exception if network request fails
     */
    suspend fun fetchDailyEnergyData(
        startDate: String,
        endDate: String,
        deviceId: String
    ): String {
        val url = "$baseUrl/get_energy_data.php?device_id=$deviceId&date_start=$startDate&date_end=$endDate"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("HTTP error: ${response.code}")
        }
        
        return response.body?.string() ?: throw Exception("Empty response body")
    }
    
    companion object {
        @Volatile
        private var instance: EnergyApiService? = null
        
        fun getInstance(context: Context): EnergyApiService {
            return instance ?: synchronized(this) {
                instance ?: EnergyApiService(context.applicationContext).also { instance = it }
            }
        }
    }
}
