package com.example.energy20.repository

import com.example.energy20.data.DeviceEnergyData
import com.example.energy20.data.DeviceInfo
import com.example.energy20.data.DeviceUsageResponse
import com.example.energy20.network.EnergyApiService
import com.example.energy20.utils.HtmlJsonExtractor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for accessing energy consumption data
 */
class EnergyRepository(
    private val apiService: EnergyApiService = EnergyApiService.getInstance()
) {
    
    private val gson = Gson()
    
    /**
     * Fetches daily energy data for the specified date range
     * 
     * @param startDate Date in YYYY-MM-DD format
     * @param endDate Date in YYYY-MM-DD format
     * @return Result containing DeviceEnergyData or error
     */
    suspend fun getDailyEnergyData(
        startDate: String,
        endDate: String
    ): Result<DeviceEnergyData> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch HTML page
            val html = apiService.fetchDailyConsumptionPage(startDate, endDate)
            
            // 2. Extract JSON from HTML
            val jsonString = HtmlJsonExtractor.extractDailyData(html)
                ?: return@withContext Result.failure(Exception("Could not extract JSON from HTML"))
            
            // 3. Parse JSON to Kotlin objects
            val type = object : TypeToken<Map<String, DeviceInfo>>() {}.type
            val data: DeviceEnergyData = gson.fromJson(jsonString, type)
            
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetches device usage data including settings and energy summary
     * 
     * @param deviceId The device ID to fetch data for
     * @return Result containing DeviceUsageResponse or error
     */
    suspend fun getDeviceUsageData(
        deviceId: String = "D072F19EF0C8"
    ): Result<DeviceUsageResponse> = withContext(Dispatchers.IO) {
        try {
            // Fetch JSON data
            val jsonString = apiService.fetchDeviceUsageData(deviceId)
            
            // Parse JSON to Kotlin object
            val data: DeviceUsageResponse = gson.fromJson(jsonString, DeviceUsageResponse::class.java)
            
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
