package com.example.energy20.network

import com.example.energy20.data.WeatherData
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to fetch weather data from Open-Meteo API
 * Open-Meteo is a free weather API with no API key required
 */
class WeatherApiService(private val client: OkHttpClient) {
    
    private val gson = Gson()
    
    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1"
        
        // Paphos, Cyprus coordinates (hardcoded for now)
        private const val PAPHOS_LATITUDE = 34.77
        private const val PAPHOS_LONGITUDE = 32.42
    }
    
    /**
     * Fetch historical weather data for a date range
     * @param startDate Date in YYYY-MM-DD format
     * @param endDate Date in YYYY-MM-DD format
     * @return Result containing WeatherData or error
     */
    suspend fun getHistoricalWeather(
        startDate: String,
        endDate: String,
        latitude: Double = PAPHOS_LATITUDE,
        longitude: Double = PAPHOS_LONGITUDE,
        useFahrenheit: Boolean = false
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // Build URL with query parameters
            val tempUnit = if (useFahrenheit) "fahrenheit" else "celsius"
            val url = buildString {
                append("$BASE_URL/forecast")
                append("?latitude=$latitude")
                append("&longitude=$longitude")
                append("&daily=temperature_2m_max,temperature_2m_min")
                append("&temperature_unit=$tempUnit")
                append("&start_date=$startDate")
                append("&end_date=$endDate")
                append("&timezone=auto")
            }
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch weather data: ${response.code}")
                )
            }
            
            val body = response.body?.string() 
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val weatherData = gson.fromJson(body, WeatherData::class.java)
            Result.success(weatherData)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
