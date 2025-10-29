package com.example.reminderapp.fragments

import android.accounts.Account
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.R
import com.example.reminderapp.CalendarEventsAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminderapp.work.CalendarSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class CalendarFragment : Fragment() {
    private lateinit var recyclerViewEvents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewStatus: TextView
    private lateinit var buttonSignIn: Button
    private lateinit var eventsAdapter: CalendarEventsAdapter
    private lateinit var googleSignInClient: GoogleSignInClient
    private var calendarService: Calendar? = null

    companion object {
        private const val RC_SIGN_IN = 1000
        private const val PREF_ACCOUNT_NAME = "accountName"
        private const val REQ_FOREGROUND_LOCATION = 2001
        private const val REQ_BACKGROUND_LOCATION = 2002
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupRecyclerView()
        setupGoogleCalendar()
        setupClickListeners()
        // Ensure we have location permissions (foreground then background)
        checkAndRequestLocationPermissions()
    }

    private fun checkAndRequestLocationPermissions() {
        // Foreground location (fine) is required first
        val fine = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            // Request foreground location permissions
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_FOREGROUND_LOCATION
            )
            return
        }

        // Foreground granted; now request background if needed and supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val background = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (background != PackageManager.PERMISSION_GRANTED) {
                // Show a short rationale explaining why background access is needed, then request
                AlertDialog.Builder(requireContext())
                    .setTitle("Background location")
                    .setMessage("Allowing background location lets the app detect location-based reminders even when the app is not in use. Please allow 'All the time' on the next screen.")
                    .setPositiveButton("Continue") { _, _ ->
                        requestPermissions(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQ_BACKGROUND_LOCATION)
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_FOREGROUND_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                    // Foreground granted; try requesting background next
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        checkAndRequestLocationPermissions()
                    }
                } else {
                    // Foreground denied
                    AlertDialog.Builder(requireContext())
                        .setTitle("Location required")
                        .setMessage("This feature needs location permission to work. You can grant it from app settings.")
                        .setPositiveButton("Open settings") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", requireContext().packageName, null)
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            REQ_BACKGROUND_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Background granted
                    Toast.makeText(requireContext(), "Background location granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Background denied - show settings option because some devices require Settings flow
                    AlertDialog.Builder(requireContext())
                        .setTitle("Allow all the time")
                        .setMessage("To receive location reminders while the app is not running, please allow 'All the time' in App settings.")
                        .setPositiveButton("Open settings") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", requireContext().packageName, null)
                            startActivity(intent)
                        }
                        .setNegativeButton("Not now", null)
                        .show()
                }
            }
        }
    }

    private fun initializeViews(view: View) {
        recyclerViewEvents = view.findViewById(R.id.recyclerViewEvents)
        progressBar = view.findViewById(R.id.progressBar)
        textViewStatus = view.findViewById(R.id.textViewStatus)
        buttonSignIn = view.findViewById(R.id.buttonSignIn)
        Log.d("CalendarFragment", "Views initialized")
    }

    private fun setupRecyclerView() {
        eventsAdapter = CalendarEventsAdapter()
        recyclerViewEvents.apply {
            adapter = eventsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupGoogleCalendar() {
        // Configure Google Sign-In with Calendar readonly scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.calendar.CalendarScopes.CALENDAR_READONLY))
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            initializeCalendarService(account)
        } else {
            showSignInButton()
        }
    }

    private fun setupClickListeners() {
        buttonSignIn.setOnClickListener {
            chooseAccount()
        }
    }

    private fun chooseAccount() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun initializeCalendarService(account: GoogleSignInAccount) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
                        requireContext(),
                        Collections.singleton(com.google.api.services.calendar.CalendarScopes.CALENDAR_READONLY)
                    )
                    credential.selectedAccountName = account.email
                    
                    calendarService = Calendar.Builder(
                        NetHttpTransport(),
                        GsonFactory(),
                        credential
                    )
                        .setApplicationName("Nudge Me Calendar")
                        .build()
                }
                loadCalendarEvents()
            } catch (e: Exception) {
                Log.e("CalendarFragment", "Error initializing calendar service", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to initialize calendar service: ${e.message}")
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun loadCalendarEvents() {
        lifecycleScope.launch {
            try {
                if (!isNetworkAvailable()) {
                    showError("No internet connection. Please check your network settings and try again.")
                    return@launch
                }

                showLoading(true)
                withContext(Dispatchers.IO) {
                    val now = java.util.Calendar.getInstance()
                    val timeMin = now.time
                    now.add(java.util.Calendar.MONTH, 1)
                    val timeMax = now.time

                    val service = calendarService ?: return@withContext
                    val serviceList = service.calendarList().list().execute().items ?: emptyList()

                    val primary = service.events()
                        .list("primary")
                        .setTimeMin(com.google.api.client.util.DateTime(timeMin))
                        .setTimeMax(com.google.api.client.util.DateTime(timeMax))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()
                        .items ?: emptyList()

                    // Try to find Birthdays calendar
                    val birthdayList = try {
                        val bday = serviceList.firstOrNull { it.summary?.contains("Birthday", true) == true }
                        if (bday != null) {
                            service.events().list(bday.id)
                                .setTimeMin(com.google.api.client.util.DateTime(timeMin))
                                .setTimeMax(com.google.api.client.util.DateTime(timeMax))
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute()
                                .items ?: emptyList()
                        } else emptyList()
                    } catch (_: Exception) { emptyList() }

                    // Try to find Tasks calendar (if user has Tasks calendar linked)
                    val tasksList = try {
                        val tasksCal = serviceList.firstOrNull { it.summary?.contains("Tasks", true) == true }
                        if (tasksCal != null) {
                            service.events().list(tasksCal.id)
                                .setTimeMin(com.google.api.client.util.DateTime(timeMin))
                                .setTimeMax(com.google.api.client.util.DateTime(timeMax))
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute()
                                .items ?: emptyList()
                        } else emptyList()
                    } catch (_: Exception) { emptyList() }

                    val allEvents = primary + birthdayList + tasksList

                    // Map to CalendarItem model
                    val calendarItems = allEvents.map { ev ->
                        val title = ev.summary ?: "(No title)"
                        val startMillis = when {
                            ev.start?.dateTime != null -> ev.start.dateTime.value
                            ev.start?.date != null -> ev.start.date.value
                            else -> null
                        }
                        val isBirthday = title.contains("birthday", ignoreCase = true)
                        val type = when {
                            isBirthday -> com.example.reminderapp.models.CalendarItem.ItemType.BIRTHDAY
                            // If the event originates from the Tasks calendar we labelled earlier, prefer TASK
                                ev.organizer?.email?.contains("tasks.calendar.google.com") == true || 
                                ev.organizer?.displayName?.contains("Tasks", true) == true ||
                                tasksList.contains(ev) || 
                                title.contains("Task:", true) -> com.example.reminderapp.models.CalendarItem.ItemType.TASK
                            else -> com.example.reminderapp.models.CalendarItem.ItemType.EVENT
                        }

                        com.example.reminderapp.models.CalendarItem(
                            id = ev.id ?: UUID.randomUUID().toString(),
                            title = title,
                            startMillis = startMillis,
                            isAllDay = ev.start?.date != null,
                            description = ev.description,
                            type = type
                        )
                    }

                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        if (calendarItems.isEmpty()) {
                            showMessage("No upcoming events found")
                        } else {
                            eventsAdapter.updateEvents(calendarItems)
                            recyclerViewEvents.visibility = View.VISIBLE
                            textViewStatus.visibility = View.GONE
                            buttonSignIn.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CalendarFragment", "Error loading calendar events", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    val errorMessage = when {
                        e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                            "Cannot connect to Google servers. Please check your internet connection and try again."
                        e.message?.contains("invalid_grant", ignoreCase = true) == true ->
                            "Your Google Calendar access has expired. Please sign in again."
                        e.message?.contains("unauthorized", ignoreCase = true) == true ->
                            "Not authorized to access calendar. Please sign in again."
                        else -> "Failed to load events: ${e.message}"
                    }
                    showError(errorMessage)
                    
                    // If authentication error, clear credentials and show sign in
                    if (errorMessage.contains("sign in again")) {
                        googleSignInClient.signOut().addOnCompleteListener {
                            showSignInButton()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        recyclerViewEvents.visibility = if (loading) View.GONE else View.VISIBLE
    }

    private fun showMessage(message: String) {
        textViewStatus.text = message
        textViewStatus.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        textViewStatus.text = message
        textViewStatus.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showSignInButton() {
        buttonSignIn.visibility = View.VISIBLE
        textViewStatus.text = "Sign in to view your Google Calendar events"
        textViewStatus.visibility = View.VISIBLE
        recyclerViewEvents.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        // Save account info
                        requireContext().getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString(PREF_ACCOUNT_NAME, account.email)
                            .apply()
                        
                        buttonSignIn.visibility = View.GONE
                        textViewStatus.visibility = View.GONE
                        initializeCalendarService(account)

                        // Trigger immediate background sync
                        WorkManager.getInstance(requireContext()).enqueue(
                            OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
                        )
                    }
                } catch (e: ApiException) {
                    val code = e.statusCode
                    val codeString = GoogleSignInStatusCodes.getStatusCodeString(code)
                    Log.e("CalendarFragment", "Sign-in failed ($code: $codeString)", e)
                    showError("Sign-in failed ($code: $codeString)")
                }
            }
        }
    }
}
