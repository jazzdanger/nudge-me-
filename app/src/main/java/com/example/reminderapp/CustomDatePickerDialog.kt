package com.example.reminderapp

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class CustomDatePickerDialog : DialogFragment() {
    
    private var listener: OnDateSetListener? = null
    private var initialYear: Int = 2024
    private var initialMonth: Int = 11
    private var initialDay: Int = 15
    
    interface OnDateSetListener {
        fun onDateSet(year: Int, month: Int, dayOfMonth: Int)
    }
    
    companion object {
        fun newInstance(
            year: Int,
            month: Int,
            dayOfMonth: Int
        ): CustomDatePickerDialog {
            val dialog = CustomDatePickerDialog()
            dialog.initialYear = year
            dialog.initialMonth = month
            dialog.initialDay = dayOfMonth
            return dialog
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.custom_date_picker, null)
        
        setupDatePicker(view)
        setupQuickButtons(view)
        setupCloseButton(view)
        
        builder.setView(view)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        return dialog
    }
    
    private fun setupDatePicker(view: android.view.View) {
        val datePicker = view.findViewById<DatePicker>(R.id.datePicker)
        val selectedDateText = view.findViewById<TextView>(R.id.txtSelectedDate)
        
        datePicker.init(initialYear, initialMonth, initialDay) { _, year, month, dayOfMonth ->
            updateSelectedDateDisplay(selectedDateText, year, month, dayOfMonth)
        }
        
        // Initial display
        updateSelectedDateDisplay(selectedDateText, initialYear, initialMonth, initialDay)
    }
    
    private fun updateSelectedDateDisplay(textView: TextView, year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        val dateString = when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 -> "Tomorrow"
            else -> dateFormat.format(calendar.time)
        }
        
        textView.text = dateString
    }
    
    private fun setupQuickButtons(view: android.view.View) {
        val btnToday = view.findViewById<Button>(R.id.btnToday)
        val btnTomorrow = view.findViewById<Button>(R.id.btnTomorrow)
        val btnNextWeek = view.findViewById<Button>(R.id.btnNextWeek)
        
        btnToday.setOnClickListener {
            val today = Calendar.getInstance()
            setDateFromCalendar(today)
            dismiss()
        }
        
        btnTomorrow.setOnClickListener {
            val tomorrow = Calendar.getInstance()
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            setDateFromCalendar(tomorrow)
            dismiss()
        }
        
        btnNextWeek.setOnClickListener {
            val nextWeek = Calendar.getInstance()
            nextWeek.add(Calendar.WEEK_OF_YEAR, 1)
            setDateFromCalendar(nextWeek)
            dismiss()
        }
    }
    
    private fun setDateFromCalendar(calendar: Calendar) {
        listener?.onDateSet(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    private fun setupCloseButton(view: android.view.View) {
        val btnClose = view.findViewById<android.widget.ImageView>(R.id.btnCloseDate)
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    fun setOnDateSetListener(listener: OnDateSetListener?) {
        this.listener = listener
    }
}
