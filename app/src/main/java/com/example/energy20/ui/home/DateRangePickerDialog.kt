package com.example.energy20.ui.home

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.energy20.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class DateRangePickerDialog : DialogFragment() {
    
    private var onDateRangeSelected: ((startDate: Calendar, endDate: Calendar) -> Unit)? = null
    private var startDateFromApi: String? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    companion object {
        fun newInstance(startDateFromApi: String?, callback: (Calendar, Calendar) -> Unit): DateRangePickerDialog {
            return DateRangePickerDialog().apply {
                this.startDateFromApi = startDateFromApi
                this.onDateRangeSelected = callback
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_range_picker, null)
        
        setupQuickOptions(view)
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Date Range Picker")
            .setView(view)
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    private fun setupQuickOptions(view: View) {
        val optionsLayout = view.findViewById<LinearLayout>(R.id.optionsLayout)
        
        // Since start date option
        if (startDateFromApi != null) {
            addOption(optionsLayout, "Since start date") {
                try {
                    val start = Calendar.getInstance().apply {
                        time = dateFormat.parse(startDateFromApi!!)!!
                    }
                    val end = Calendar.getInstance()
                    onDateRangeSelected?.invoke(start, end)
                    dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid start date", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Last week
        addOption(optionsLayout, "Last week") {
            val end = Calendar.getInstance()
            val start = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }
            onDateRangeSelected?.invoke(start, end)
            dismiss()
        }
        
        // Last 2 weeks
        addOption(optionsLayout, "Last 2 weeks") {
            val end = Calendar.getInstance()
            val start = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -14)
            }
            onDateRangeSelected?.invoke(start, end)
            dismiss()
        }
        
        // Last month
        addOption(optionsLayout, "Last month") {
            val end = Calendar.getInstance()
            val start = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }
            onDateRangeSelected?.invoke(start, end)
            dismiss()
        }
        
        // Start and end dates
        addOption(optionsLayout, "Start and end dates →") {
            showStartEndDatePicker()
        }
        
        // Around a date
        addOption(optionsLayout, "Around a date →") {
            showAroundDatePicker()
        }
    }
    
    private fun addOption(parent: LinearLayout, text: String, onClick: () -> Unit) {
        val button = TextView(requireContext()).apply {
            this.text = text
            textSize = 16f
            setPadding(32, 32, 32, 32)
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { onClick() }
        }
        parent.addView(button)
    }
    
    private fun showStartEndDatePicker() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_start_end_dates, null)
        
        val startDateButton = dialogView.findViewById<Button>(R.id.selectStartDateButton)
        val endDateButton = dialogView.findViewById<Button>(R.id.selectEndDateButton)
        val startDateText = dialogView.findViewById<TextView>(R.id.startDateText)
        val endDateText = dialogView.findViewById<TextView>(R.id.endDateText)
        
        var selectedStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        var selectedEnd = Calendar.getInstance()
        
        startDateText.text = displayDateFormat.format(selectedStart.time)
        endDateText.text = displayDateFormat.format(selectedEnd.time)
        
        startDateButton.setOnClickListener {
            showDatePicker(selectedStart) { date ->
                selectedStart = date
                startDateText.text = displayDateFormat.format(date.time)
            }
        }
        
        endDateButton.setOnClickListener {
            showDatePicker(selectedEnd) { date ->
                selectedEnd = date
                endDateText.text = displayDateFormat.format(date.time)
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Start and End Dates")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                onDateRangeSelected?.invoke(selectedStart, selectedEnd)
                dismiss()
            }
            .setNegativeButton("Back", null)
            .show()
    }
    
    private fun showAroundDatePicker() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_around_date, null)
        
        val selectDateButton = dialogView.findViewById<Button>(R.id.selectCenterDateButton)
        val centerDateText = dialogView.findViewById<TextView>(R.id.centerDateText)
        val intervalSeekBar = dialogView.findViewById<SeekBar>(R.id.intervalSeekBar)
        val intervalText = dialogView.findViewById<TextView>(R.id.intervalText)
        val resultText = dialogView.findViewById<TextView>(R.id.resultText)
        
        var selectedDate = Calendar.getInstance()
        var interval = 5
        
        centerDateText.text = displayDateFormat.format(selectedDate.time)
        intervalText.text = "± $interval days"
        updateResultText(resultText, selectedDate, interval)
        
        selectDateButton.setOnClickListener {
            showDatePicker(selectedDate) { date ->
                selectedDate = date
                centerDateText.text = displayDateFormat.format(date.time)
                updateResultText(resultText, selectedDate, interval)
            }
        }
        
        intervalSeekBar.max = 30
        intervalSeekBar.progress = interval
        intervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                interval = progress.coerceAtLeast(1)
                intervalText.text = "± $interval days"
                updateResultText(resultText, selectedDate, interval)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Around a Date")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val start = selectedDate.clone() as Calendar
                start.add(Calendar.DAY_OF_YEAR, -interval)
                val end = selectedDate.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, interval)
                onDateRangeSelected?.invoke(start, end)
                dismiss()
            }
            .setNegativeButton("Back", null)
            .show()
    }
    
    private fun updateResultText(textView: TextView, centerDate: Calendar, interval: Int) {
        val start = centerDate.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -interval)
        val end = centerDate.clone() as Calendar
        end.add(Calendar.DAY_OF_YEAR, interval)
        
        textView.text = "Range: ${displayDateFormat.format(start.time)} to ${displayDateFormat.format(end.time)}"
    }
    
    private fun showDatePicker(currentDate: Calendar, onDateSelected: (Calendar) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                onDateSelected(selected)
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
