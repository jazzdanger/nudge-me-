package com.example.reminderapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class CustomTimePickerDialog : DialogFragment() {
    
    private var listener: OnTimeSetListener? = null
    private var initialHour: Int = 11
    private var initialMinute: Int = 0
    private var is24HourFormat: Boolean = false
    
    interface OnTimeSetListener {
        fun onTimeSet(hourOfDay: Int, minute: Int)
    }
    
    companion object {
        fun newInstance(
            hourOfDay: Int,
            minute: Int,
            is24HourFormat: Boolean = false
        ): CustomTimePickerDialog {
            val dialog = CustomTimePickerDialog()
            dialog.initialHour = hourOfDay
            dialog.initialMinute = minute
            dialog.is24HourFormat = is24HourFormat
            return dialog
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.custom_time_picker, null)
        
        setupTimePickers(view)
        setupQuickButtons(view)
        setupCloseButton(view)
        
        builder.setView(view)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        return dialog
    }
    
    private fun setupTimePickers(view: android.view.View) {
        val hourPicker = view.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = view.findViewById<NumberPicker>(R.id.minutePicker)
        val ampmPicker = view.findViewById<NumberPicker>(R.id.ampmPicker)
        val currentTimeText = view.findViewById<TextView>(R.id.txtCurrentTime)
        
        // Setup hour picker
        if (is24HourFormat) {
            hourPicker.minValue = 0
            hourPicker.maxValue = 23
            hourPicker.value = initialHour
            ampmPicker.visibility = android.view.View.GONE
        } else {
            hourPicker.minValue = 1
            hourPicker.maxValue = 12
            hourPicker.value = if (initialHour == 0) 12 else if (initialHour > 12) initialHour - 12 else initialHour
            ampmPicker.minValue = 0
            ampmPicker.maxValue = 1
            ampmPicker.displayedValues = arrayOf("am", "pm")
            ampmPicker.value = if (initialHour < 12) 0 else 1
        }
        
        // Setup minute picker
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.value = initialMinute
        
        // Update current time display
        updateCurrentTimeDisplay(currentTimeText, hourPicker.value, minutePicker.value, ampmPicker.value)
        
        // Add listeners to update display
        hourPicker.setOnValueChangedListener { _, _, newVal ->
            updateCurrentTimeDisplay(currentTimeText, newVal, minutePicker.value, ampmPicker.value)
        }
        
        minutePicker.setOnValueChangedListener { _, _, newVal ->
            updateCurrentTimeDisplay(currentTimeText, hourPicker.value, newVal, ampmPicker.value)
        }
        
        ampmPicker.setOnValueChangedListener { _, _, newVal ->
            updateCurrentTimeDisplay(currentTimeText, hourPicker.value, minutePicker.value, newVal)
        }
    }
    
    private fun updateCurrentTimeDisplay(textView: TextView, hour: Int, minute: Int, ampm: Int) {
        val timeFormat = if (is24HourFormat) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
        
        val calendar = Calendar.getInstance()
        if (is24HourFormat) {
            calendar.set(Calendar.HOUR_OF_DAY, hour)
        } else {
            val hourOfDay = if (hour == 12) {
                if (ampm == 0) 0 else 12
            } else {
                if (ampm == 0) hour else hour + 12
            }
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        }
        calendar.set(Calendar.MINUTE, minute)
        
        textView.text = timeFormat.format(calendar.time)
    }
    
    private fun setupQuickButtons(view: android.view.View) {
        val btnOneHour = view.findViewById<Button>(R.id.btnOneHour)
        val btnSevenAM = view.findViewById<Button>(R.id.btnSevenAM)
        val btnThreePM = view.findViewById<Button>(R.id.btnThreePM)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmTime)
        
        btnOneHour.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            setTimeFromCalendar(calendar)
            dismiss()
        }
        
        btnSevenAM.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
            setTimeFromCalendar(calendar)
            dismiss()
        }
        
        btnThreePM.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 15)
            calendar.set(Calendar.MINUTE, 0)
            setTimeFromCalendar(calendar)
            dismiss()
        }

        btnConfirm.setOnClickListener {
            val hourPicker = view.findViewById<NumberPicker>(R.id.hourPicker)
            val minutePicker = view.findViewById<NumberPicker>(R.id.minutePicker)
            val ampmPicker = view.findViewById<NumberPicker>(R.id.ampmPicker)
            val hourOfDay = if (is24HourFormat) {
                hourPicker.value
            } else {
                val base = if (hourPicker.value == 12) 0 else hourPicker.value
                if (ampmPicker.value == 0) base else base + 12
            }
            listener?.onTimeSet(hourOfDay, minutePicker.value)
            dismiss()
        }
    }
    
    private fun setTimeFromCalendar(calendar: Calendar) {
        listener?.onTimeSet(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
    
    private fun setupCloseButton(view: android.view.View) {
        val btnClose = view.findViewById<android.widget.ImageView>(R.id.btnClose)
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    fun setOnTimeSetListener(listener: OnTimeSetListener?) {
        this.listener = listener
    }
}
