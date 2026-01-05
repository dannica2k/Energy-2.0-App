package com.example.energy20.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.energy20.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    companion object {
        private const val PREFS_NAME = "energy_settings"
        private const val KEY_OCCUPIED_THRESHOLD = "occupied_threshold"
        private const val KEY_LATITUDE = "weather_latitude"
        private const val KEY_LONGITUDE = "weather_longitude"
        private const val KEY_TEMP_UNIT = "temperature_unit"
        
        private const val DEFAULT_THRESHOLD = 1.0
        private const val DEFAULT_LATITUDE = 34.77 // Paphos, Cyprus
        private const val DEFAULT_LONGITUDE = 32.42
        private const val TEMP_UNIT_CELSIUS = "celsius"
        private const val TEMP_UNIT_FAHRENHEIT = "fahrenheit"
        
        fun getOccupiedThreshold(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_OCCUPIED_THRESHOLD, DEFAULT_THRESHOLD.toFloat()).toDouble()
        }
        
        fun setOccupiedThreshold(context: Context, threshold: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_OCCUPIED_THRESHOLD, threshold.toFloat()).apply()
        }
        
        fun getLatitude(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_LATITUDE, DEFAULT_LATITUDE.toFloat()).toDouble()
        }
        
        fun getLongitude(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_LONGITUDE, DEFAULT_LONGITUDE.toFloat()).toDouble()
        }
        
        fun setLocation(context: Context, latitude: Double, longitude: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat(KEY_LATITUDE, latitude.toFloat())
                .putFloat(KEY_LONGITUDE, longitude.toFloat())
                .apply()
        }
        
        fun isCelsius(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_TEMP_UNIT, TEMP_UNIT_CELSIUS) == TEMP_UNIT_CELSIUS
        }
        
        fun setTemperatureUnit(context: Context, isCelsius: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val unit = if (isCelsius) TEMP_UNIT_CELSIUS else TEMP_UNIT_FAHRENHEIT
            prefs.edit().putString(KEY_TEMP_UNIT, unit).apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        setupUI()
        loadSettings()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
        
        binding.saveWeatherButton.setOnClickListener {
            saveWeatherSettings()
        }
    }
    
    private fun loadSettings() {
        // Load occupancy threshold
        val threshold = getOccupiedThreshold(requireContext())
        binding.thresholdInput.setText(threshold.toString())
        binding.currentValueText.text = "Current: $threshold kWh"
        
        // Load temperature unit
        val isCelsius = isCelsius(requireContext())
        if (isCelsius) {
            binding.celsiusRadio.isChecked = true
        } else {
            binding.fahrenheitRadio.isChecked = true
        }
    }
    
    private fun saveSettings() {
        val thresholdText = binding.thresholdInput.text.toString()
        
        if (thresholdText.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a threshold value", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        try {
            val threshold = thresholdText.toDouble()
            
            if (threshold < 0) {
                Snackbar.make(binding.root, "Threshold must be positive", Snackbar.LENGTH_SHORT).show()
                return
            }
            
            setOccupiedThreshold(requireContext(), threshold)
            binding.currentValueText.text = "Current: $threshold kWh"
            
            // Show success message
            binding.successText.visibility = View.VISIBLE
            binding.successText.postDelayed({
                binding.successText.visibility = View.GONE
            }, 3000)
            
            Snackbar.make(binding.root, "Settings saved successfully!", Snackbar.LENGTH_SHORT).show()
            
        } catch (e: NumberFormatException) {
            Snackbar.make(binding.root, "Please enter a valid number", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun saveWeatherSettings() {
        // Save temperature unit
        val isCelsius = binding.celsiusRadio.isChecked
        setTemperatureUnit(requireContext(), isCelsius)
        
        // Show success message
        binding.successText.visibility = View.VISIBLE
        binding.successText.postDelayed({
            binding.successText.visibility = View.GONE
        }, 3000)
        
        Snackbar.make(binding.root, "Weather settings saved! Reload data to see changes.", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
