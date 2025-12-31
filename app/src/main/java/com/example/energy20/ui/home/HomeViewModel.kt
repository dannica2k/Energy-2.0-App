package com.example.energy20.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.energy20.data.DeviceEnergyData
import com.example.energy20.data.DeviceUsageResponse
import com.example.energy20.data.DailyTemperature
import com.example.energy20.repository.EnergyRepository
import com.example.energy20.ui.settings.SettingsFragment
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EnergyRepository()
    private val context = application.applicationContext
    
    private val _energyData = MutableLiveData<DeviceEnergyData>()
    val energyData: LiveData<DeviceEnergyData> = _energyData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _deviceUsage = MutableLiveData<DeviceUsageResponse>()
    val deviceUsage: LiveData<DeviceUsageResponse> = _deviceUsage
    
    private val _weatherData = MutableLiveData<List<DailyTemperature>>()
    val weatherData: LiveData<List<DailyTemperature>> = _weatherData
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    init {
        // Load device usage data
        loadDeviceUsage()
        // Load data for last 7 days by default
        loadEnergyData()
    }
    
    /**
     * Loads energy data for the specified date range
     * If no dates provided, uses last 7 days
     * Also loads weather data for the same date range
     */
    fun loadEnergyData(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val calendar = Calendar.getInstance()
                val end = endDate ?: dateFormat.format(calendar.time)
                
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val start = startDate ?: dateFormat.format(calendar.time)
                
                // Load energy data
                val energyResult = repository.getDailyEnergyData(start, end)
                
                energyResult.onSuccess { data ->
                    _energyData.value = data
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load energy data"
                }
                
                // Load weather data for the same date range with settings
                val latitude = SettingsFragment.getLatitude(context)
                val longitude = SettingsFragment.getLongitude(context)
                val useFahrenheit = !SettingsFragment.isCelsius(context)
                
                Log.d("HomeViewModel", "=== WEATHER DATA REQUEST ===")
                Log.d("HomeViewModel", "Date range: $start to $end")
                Log.d("HomeViewModel", "Latitude: $latitude, Longitude: $longitude")
                Log.d("HomeViewModel", "Use Fahrenheit: $useFahrenheit")
                
                val weatherResult = repository.getWeatherData(start, end, latitude, longitude, useFahrenheit)
                
                weatherResult.onSuccess { data ->
                    Log.d("HomeViewModel", "Weather data received: ${data.size} days")
                    data.forEachIndexed { index, temp ->
                        Log.d("HomeViewModel", "Day $index: ${temp.date} - Avg: ${temp.avgTemp}°, Min: ${temp.minTemp}°, Max: ${temp.maxTemp}°")
                    }
                    _weatherData.value = data
                }.onFailure { error ->
                    Log.e("HomeViewModel", "Weather data failed: ${error.message}", error)
                    _errorMessage.value = "Weather: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Loads device usage data including settings and energy summary
     */
    fun loadDeviceUsage(deviceId: String = "D072F19EF0C8") {
        viewModelScope.launch {
            try {
                val result = repository.getDeviceUsageData(deviceId)
                
                result.onSuccess { data ->
                    _deviceUsage.value = data
                }.onFailure { error ->
                    // Silently fail for device usage - not critical
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    /**
     * Clears the current error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
