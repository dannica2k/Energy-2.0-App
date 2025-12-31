package com.example.energy20.data

/**
 * Weather data from Open-Meteo API
 */
data class WeatherData(
    val daily: DailyWeather
)

data class DailyWeather(
    val time: List<String>,  // Dates in YYYY-MM-DD format
    val temperature_2m_max: List<Double>,  // Max temperature in Celsius
    val temperature_2m_min: List<Double>   // Min temperature in Celsius
)

/**
 * Processed weather data for a specific date
 */
data class DailyTemperature(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val avgTemp: Double
) {
    companion object {
        fun fromWeatherData(weatherData: WeatherData): List<DailyTemperature> {
            val result = mutableListOf<DailyTemperature>()
            
            for (i in weatherData.daily.time.indices) {
                val date = weatherData.daily.time[i]
                val maxTemp = weatherData.daily.temperature_2m_max[i]
                val minTemp = weatherData.daily.temperature_2m_min[i]
                val avgTemp = (maxTemp + minTemp) / 2.0
                
                result.add(DailyTemperature(date, maxTemp, minTemp, avgTemp))
            }
            
            return result
        }
    }
}
