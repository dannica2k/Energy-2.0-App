package com.example.energy20.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Service for fetching energy data from the PHP backend
 */
class EnergyApiService {
    
    private val client: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Fetches device usage data including settings and energy summary
     * 
     * @param deviceId The device ID to fetch data for
     * @return JSON string with device usage data
     * @throws Exception if network request fails
     */
    suspend fun fetchDeviceUsageData(
        deviceId: String = "D072F19EF0C8"
    ): String {
        val url = "http://energy.webmenow.ca/get_usage_data.php?device_id=$deviceId"
        
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
     * Fetches the daily consumption HTML page
     * 
     * @param startDate Date in YYYY-MM-DD format
     * @param endDate Date in YYYY-MM-DD format
     * @return HTML content as string
     * @throws Exception if network request fails
     */
    suspend fun fetchDailyConsumptionPage(
        startDate: String,
        endDate: String,
        deviceId: String = "D072F19EF0C8"
    ): String {
        val url = "http://energy.webmenow.ca/?date_start=$startDate&date_end=$endDate&device_id=$deviceId"
        
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
        
        fun getInstance(): EnergyApiService {
            return instance ?: synchronized(this) {
                instance ?: EnergyApiService().also { instance = it }
            }
        }
    }
}
