package com.example.energy20.ui.home

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.energy20.R
import com.example.energy20.data.DeviceEnergyData
import com.example.energy20.data.DailyTemperature
import com.example.energy20.databinding.FragmentHomeBinding
import com.example.energy20.ui.settings.SettingsFragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: HomeViewModel
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    private var startDate: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    private var endDate: Calendar = Calendar.getInstance()
    
    // Color palette matching the web version
    private val colorPalette = listOf(
        Color.parseColor("#3b82f6"), // Blue
        Color.parseColor("#8b5cf6"), // Purple
        Color.parseColor("#f59e0b"), // Orange
        Color.parseColor("#ef4444"), // Red
        Color.parseColor("#10b981"), // Green
        Color.parseColor("#06b6d4")  // Cyan
    )
    
    // Temperature color (distinct from energy colors)
    private val temperatureColor = Color.parseColor("#ff6b6b") // Warm red for temperature
    
    // Store weather data for chart updates
    private var currentWeatherData: List<DailyTemperature>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        setHasOptionsMenu(true)
        
        setupUI()
        setupObservers()
        setupChart()
        
        return binding.root
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Menu is already inflated by MainActivity, we just need to handle clicks
        super.onCreateOptionsMenu(menu, inflater)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_since_start -> {
                loadSinceStart()
                true
            }
            R.id.action_last_week -> {
                loadLastWeek()
                true
            }
            R.id.action_last_2weeks -> {
                loadLast2Weeks()
                true
            }
            R.id.action_last_month -> {
                loadLastMonth()
                true
            }
            R.id.action_custom_range -> {
                showCustomRangePicker()
                true
            }
            R.id.action_around_date -> {
                showAroundDatePicker()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun loadSinceStart() {
        val startDateFromApi = viewModel.deviceUsage.value?.energySummary?.startDate
        if (startDateFromApi != null) {
            startDate = Calendar.getInstance().apply {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startDateFromApi)!!
            }
            endDate = Calendar.getInstance()
            loadData()
        }
    }
    
    private fun loadLastWeek() {
        endDate = Calendar.getInstance()
        startDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        loadData()
    }
    
    private fun loadLast2Weeks() {
        endDate = Calendar.getInstance()
        startDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -14) }
        loadData()
    }
    
    private fun loadLastMonth() {
        endDate = Calendar.getInstance()
        startDate = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        loadData()
    }
    
    private fun showCustomRangePicker() {
        // The DateRangePickerDialog already has "Start and end dates" option
        // Just show the dialog and it will handle the rest
        DateRangePickerDialog.newInstance(null) { start, end ->
            startDate = start
            endDate = end
            loadData()
        }.show(childFragmentManager, "DateRangePicker")
    }
    
    private fun showAroundDatePicker() {
        // The DateRangePickerDialog already has "Around a date" option
        // Just show the dialog and it will handle the rest
        DateRangePickerDialog.newInstance(null) { start, end ->
            startDate = start
            endDate = end
            loadData()
        }.show(childFragmentManager, "DateRangePicker")
    }
    
    private fun setupUI() {
        // Pull to Refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }
    
    private fun loadData() {
        val start = dateFormat.format(startDate.time)
        val end = dateFormat.format(endDate.time)
        viewModel.loadDeviceUsage() // Refresh device usage too
        viewModel.loadEnergyData(start, end)
    }
    
    private fun setupObservers() {
        // Observe device usage data
        viewModel.deviceUsage.observe(viewLifecycleOwner) { usage ->
            usage?.let {
                // Update device info
                binding.deviceIdText.text = "Device: ${it.deviceSettings.deviceId}"
                binding.timezoneText.text = "Timezone: ${it.deviceSettings.timezoneId}"
                
                // Update statistics
                binding.statsLayout.visibility = View.VISIBLE
                binding.todayKwhText.text = String.format("%.2f kWh", it.energySummary.todayKwh)
                binding.totalKwhText.text = String.format("%.2f kWh", it.energySummary.totalKwh)
                binding.daysTrackedText.text = "${it.energySummary.daysSinceStart} days"
                
                val avgDaily = if (it.energySummary.daysSinceStart > 0) {
                    it.energySummary.totalKwh / it.energySummary.daysSinceStart
                } else {
                    0.0
                }
                binding.avgDailyText.text = String.format("%.2f kWh", avgDaily)
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
            
            if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
                binding.emptyText.visibility = View.GONE
            } else {
                binding.errorText.visibility = View.GONE
            }
        }
        
        // Observe energy data
        viewModel.energyData.observe(viewLifecycleOwner) { data ->
            if (data.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.energyChart.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.energyChart.visibility = View.VISIBLE
                updateChart(data, currentWeatherData)
                updateOccupiedDaysStats(data)
            }
        }
        
        // Observe weather data
        viewModel.weatherData.observe(viewLifecycleOwner) { weatherData ->
            currentWeatherData = weatherData
            // Re-render chart if we already have energy data
            viewModel.energyData.value?.let { energyData ->
                if (energyData.isNotEmpty()) {
                    updateChart(energyData, weatherData)
                }
            }
        }
    }
    
    private fun updateOccupiedDaysStats(data: DeviceEnergyData) {
        // Get threshold from settings
        val threshold = com.example.energy20.ui.settings.SettingsFragment.getOccupiedThreshold(requireContext())
        
        // Calculate daily totals across all devices
        val dailyTotals = mutableMapOf<String, Double>()
        
        data.values.forEach { deviceInfo ->
            deviceInfo.data.forEach { (date, kwh) ->
                dailyTotals[date] = (dailyTotals[date] ?: 0.0) + kwh
            }
        }
        
        // Count occupied days (days with consumption > threshold)
        val occupiedDays = dailyTotals.count { it.value > threshold }
        
        // Calculate occupied average
        val occupiedTotal = dailyTotals.filter { it.value > threshold }.values.sum()
        val occupiedAvg = if (occupiedDays > 0) occupiedTotal / occupiedDays else 0.0
        
        // Update UI
        binding.occupiedDaysText.text = "$occupiedDays days"
        binding.occupiedAvgText.text = String.format("%.2f kWh", occupiedAvg)
    }
    
    private fun setupChart() {
        binding.energyChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
            }
            
            // Configure left Y axis (Energy - kWh)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.GRAY
                axisMinimum = 0f
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            }
            
            // Configure right Y axis (Temperature - °C)
            axisRight.apply {
                isEnabled = true
                setDrawGridLines(false)
                textColor = temperatureColor
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                axisMinimum = 0f
                axisMaximum = 40f // Default temperature range 0-40°C
            }
            
            // Configure legend
            legend.apply {
                textColor = Color.GRAY
                textSize = 11f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            }
        }
    }
    
    private fun updateChart(data: DeviceEnergyData, weatherData: List<DailyTemperature>? = null) {
        // Collect all unique dates and sort them
        val allDates = mutableSetOf<String>()
        data.values.forEach { deviceInfo ->
            allDates.addAll(deviceInfo.data.keys)
        }
        val sortedDates = allDates.sorted()
        
        if (sortedDates.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.energyChart.visibility = View.GONE
            return
        }
        
        // Create datasets for each device (energy data)
        val dataSets = mutableListOf<LineDataSet>()
        var colorIndex = 0
        
        data.forEach { (deviceKey, deviceInfo) ->
            val entries = mutableListOf<Entry>()
            
            sortedDates.forEachIndexed { index, date ->
                val value = deviceInfo.data[date]?.toFloat() ?: 0f
                entries.add(Entry(index.toFloat(), value))
            }
            
            val dataSet = LineDataSet(entries, deviceInfo.name).apply {
                color = colorPalette[colorIndex % colorPalette.size]
                setCircleColor(colorPalette[colorIndex % colorPalette.size]
)
                lineWidth = 2.5f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                axisDependency = YAxis.AxisDependency.LEFT // Energy uses left axis
            }
            
            dataSets.add(dataSet)
            colorIndex++
        }
        
        // Add temperature dataset if available
        weatherData?.let { temps ->
            val tempEntries = mutableListOf<Entry>()
            
            sortedDates.forEachIndexed { index, date ->
                // Find matching temperature data
                val temp = temps.find { it.date == date }
                temp?.let {
                    tempEntries.add(Entry(index.toFloat(), it.avgTemp.toFloat()))
                }
            }
            
            if (tempEntries.isNotEmpty()) {
                // Get temperature unit from settings
                val isCelsius = SettingsFragment.isCelsius(requireContext())
                val tempUnit = if (isCelsius) "°C" else "°F"
                val tempLabel = "Temperature ($tempUnit)"
                
                // Adjust Y-axis range based on unit
                val minTemp = tempEntries.minOfOrNull { it.y } ?: 0f
                val maxTemp = tempEntries.maxOfOrNull { it.y } ?: 40f
                binding.energyChart.axisRight.axisMinimum = (minTemp - 5).coerceAtLeast(if (isCelsius) -10f else 10f)
                binding.energyChart.axisRight.axisMaximum = (maxTemp + 5).coerceAtMost(if (isCelsius) 50f else 120f)
                
                val tempDataSet = LineDataSet(tempEntries, tempLabel).apply {
                    color = temperatureColor
                    setCircleColor(temperatureColor)
                    lineWidth = 2.5f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    valueTextSize = 9f
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    cubicIntensity = 0.2f
                    axisDependency = YAxis.AxisDependency.RIGHT // Temperature uses right axis
                    enableDashedLine(10f, 5f, 0f) // Dashed line to distinguish from energy
                }
                
                dataSets.add(tempDataSet)
            }
        }
        
        // Format dates for X axis labels
        val chartDateFormat = SimpleDateFormat("MMM dd", Locale.US)
        val labels = sortedDates.map { dateString ->
            try {
                // Handle both "YYYY-MM-DD" and "YYYY-MM-DD HH:MM:SS" formats
                val cleanDate = dateString.split(" ")[0]
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(cleanDate)
                date?.let { chartDateFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }
        
        // Set data to chart
        val lineData = LineData(dataSets.toList())
        binding.energyChart.apply {
            this.data = lineData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = minOf(labels.size, 7)
            xAxis.setLabelRotationAngle(-45f)
            invalidate() // Refresh chart
            animateX(500)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
