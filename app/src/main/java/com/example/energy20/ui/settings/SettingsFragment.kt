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
        private const val DEFAULT_THRESHOLD = 1.0
        
        fun getOccupiedThreshold(context: Context): Double {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_OCCUPIED_THRESHOLD, DEFAULT_THRESHOLD.toFloat()).toDouble()
        }
        
        fun setOccupiedThreshold(context: Context, threshold: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_OCCUPIED_THRESHOLD, threshold.toFloat()).apply()
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
    }
    
    private fun loadSettings() {
        val threshold = getOccupiedThreshold(requireContext())
        binding.thresholdInput.setText(threshold.toString())
        binding.currentValueText.text = "Current: $threshold kWh"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
