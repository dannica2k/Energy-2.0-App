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
import com.example.energy20.auth.AuthManager
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = EnergyRepository(context)
    private val authManager = AuthManager.getInstance(context)
    
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
        // Check if user has devices
        val userDevices = authManager.getUserDevices()
        if (userDevices.isEmpty()) {
            _errorMessage.value = "No devices found. Add a device in Settings to view energy data."
            _isLoading.value = false
        } else {
            // Load device usage data for first device
            val deviceId = userDevices.first().deviceId
            loadDeviceUsage(deviceId)
            // Load data for last 7 days by default
            loadEnergyData()
        }
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
                // Get user's first device
                val userDevices = authManager.getUserDevices()
                if (userDevices.isEmpty()) {
                    _errorMessage.value = "No devices found. Add a device in Settings to view energy data."
                    _isLoading.value = false
                    return@launch
                }
                
                val deviceId = userDevices.first().deviceId
                
                val calendar = Calendar.getInstance()
                val end = endDate ?: dateFormat.format(calendar.time)
                
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val start = startDate ?: dateFormat.format(calendar.time)
                
                // Load energy data with device ID
                val energyResult = repository.getDailyEnergyData(start, end, deviceId)
                
                energyResult.onSuccess { data ->
                    _energyData.value = data
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load energy data"
                }
                
                // Load weather data for the same date range using active device's location
                val activeDevice = userDevices.find { it.isActive } ?: userDevices.first()
                val useFahrenheit = !SettingsFragment.isCelsius(context)
                
                Log.d("HomeViewModel", "=== WEATHER DATA REQUEST ===")
                Log.d("HomeViewModel", "Date range: $start to $end")
                Log.d("HomeViewModel", "Active device: ${activeDevice.deviceId} (${activeDevice.deviceName})")
                Log.d("HomeViewModel", "Device location - Lat: ${activeDevice.latitude}, Lon: ${activeDevice.longitude}")
                Log.d("HomeViewModel", "Use Fahrenheit: $useFahrenheit")
                
                // Only fetch weather if device has location set
                if (activeDevice.latitude != null && activeDevice.longitude != null) {
                    val weatherResult = repository.getWeatherData(
                        start, 
                        end, 
                        activeDevice.latitude, 
                        activeDevice.longitude, 
                        useFahrenheit
                    )
                    
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
                } else {
                    Log.d("HomeViewModel", "No location set for device - skipping weather data")
                    _weatherData.value = emptyList()
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
    fun loadDeviceUsage(deviceId: String? = null) {
        viewModelScope.launch {
            try {
                // Get device ID from user's devices or use provided one
                val userDevices = authManager.getUserDevices()
                val actualDeviceId = deviceId ?: userDevices.firstOrNull()?.deviceId
                
                if (actualDeviceId == null) {
                    _errorMessage.value = "No devices found. Add a device in Settings to view energy data."
                    return@launch
                }
                
                val result = repository.getDeviceUsageData(actualDeviceId)
                
                result.onSuccess { data ->
                    _deviceUsage.value = data
                }.onFailure { error ->
                    _errorMessage.value = "Failed to load device data: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading device: ${e.message}"
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
