package com.example.reminderapp.fragments

import android.accounts.Account
import android.content.Context
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

    private fun loadCalendarEvents() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                withContext(Dispatchers.IO) {
                    val now = java.util.Calendar.getInstance()
                    val timeMin = now.time
                    now.add(java.util.Calendar.MONTH, 1)
                    val timeMax = now.time

                    val service = calendarService ?: return@withContext
                    val primary = service.events()
                        .list("primary")
                        .setTimeMin(com.google.api.client.util.DateTime(timeMin))
                        .setTimeMax(com.google.api.client.util.DateTime(timeMax))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()
                        .items ?: emptyList()

                    val birthdayList = try {
                        val calendars = service.calendarList().list().execute().items ?: emptyList()
                        val bday = calendars.firstOrNull { it.summary?.contains("Birthday", true) == true }
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

                    val eventList = primary + birthdayList

                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        if (eventList.isEmpty()) {
                            showMessage("No upcoming events found")
                        } else {
                            eventsAdapter.updateEvents(eventList)
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
                    showError("Failed to load events: ${e.message}")
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
