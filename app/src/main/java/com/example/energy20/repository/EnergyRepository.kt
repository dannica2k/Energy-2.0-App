package com.example.energy20.repository

import android.content.Context
import com.example.energy20.data.DeviceEnergyData
import com.example.energy20.data.DeviceInfo
import com.example.energy20.data.DeviceUsageResponse
import com.example.energy20.data.DailyTemperature
import com.example.energy20.network.EnergyApiService
import com.example.energy20.network.WeatherApiService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Repository for accessing energy consumption and weather data
 */
class EnergyRepository(
    context: Context,
    private val apiService: EnergyApiService = EnergyApiService.getInstance(context),
    private val weatherApiService: WeatherApiService = WeatherApiService(OkHttpClient())
) {
    
    private val gson = Gson()
    
    /**
     * Fetches daily energy data for the specified date range
     * Uses the new authenticated API that returns pure JSON
     * 
     * @param startDate Date in YYYY-MM-DD format
     * @param endDate Date in YYYY-MM-DD format
     * @param deviceId The device ID to fetch data for
     * @return Result containing DeviceEnergyData or error
     */
    suspend fun getDailyEnergyData(
        startDate: String,
        endDate: String,
        deviceId: String
    ): Result<DeviceEnergyData> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch JSON from authenticated API
            val jsonString = apiService.fetchDailyEnergyData(startDate, endDate, deviceId)
            
            android.util.Log.d("EnergyRepository", "=== ENERGY DATA API RESPONSE ===")
            android.util.Log.d("EnergyRepository", "Request: $startDate to $endDate")
            android.util.Log.d("EnergyRepository", "Response: $jsonString")
            
            // 2. Parse the response
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            
            // 3. Extract the data field which contains the energy data
            val dataElement = jsonObject.get("data")
            
            // 4. Check if data is an object or array and handle accordingly
            val data: DeviceEnergyData = if (dataElement.isJsonObject) {
                // Single device or multiple devices as object
                val dataObject = dataElement.asJsonObject
                val type = object : TypeToken<Map<String, DeviceInfo>>() {}.type
                gson.fromJson(dataObject, type)
            } else if (dataElement.isJsonArray) {
                // Multiple devices as array - convert to map
                android.util.Log.w("EnergyRepository", "API returned array instead of object, converting...")
                emptyMap() // Return empty map for now, this shouldn't happen with device_id parameter
            } else {
                throw Exception("Unexpected data format in API response")
            }
            
            // Log the dates we got
            data.values.firstOrNull()?.data?.keys?.sorted()?.let { dates ->
                android.util.Log.d("EnergyRepository", "Dates in response: ${dates.joinToString(", ")}")
                android.util.Log.d("EnergyRepository", "Total dates: ${dates.size}")
            }
            
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
        deviceId: String
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
    
    /**
     * Fetches weather data for the specified date range
     * Currently hardcoded to Paphos, Cyprus location
     * 
     * @param startDate Date in YYYY-MM-DD format
     * @param endDate Date in YYYY-MM-DD format
     * @return Result containing list of DailyTemperature or error
     */
    suspend fun getWeatherData(
        startDate: String,
        endDate: String,
        latitude: Double = 34.77,
        longitude: Double = 32.42,
        useFahrenheit: Boolean = false
    ): Result<List<DailyTemperature>> = withContext(Dispatchers.IO) {
        try {
            val result = weatherApiService.getHistoricalWeather(
                startDate, endDate, latitude, longitude, useFahrenheit
            )
            
            if (result.isSuccess) {
                val weatherData = result.getOrNull()!!
                val temperatures = DailyTemperature.fromWeatherData(weatherData)
                Result.success(temperatures)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
