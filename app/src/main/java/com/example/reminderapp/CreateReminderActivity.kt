package com.example.reminderapp

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderRepository
import com.example.reminderapp.data.ReminderEntity
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.material.chip.Chip
import androidx.core.content.ContextCompat
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ImageButton
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateReminderActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var editTextTitle: EditText
    private lateinit var editTextDate: EditText
    private lateinit var editTextTime: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var switchNotify: SwitchMaterial
    private lateinit var chipMonday: Chip
    private lateinit var chipTuesday: Chip
    private lateinit var chipWednesday: Chip
    private lateinit var chipThursday: Chip
    private lateinit var chipFriday: Chip
    private lateinit var chipSaturday: Chip
    private lateinit var chipSunday: Chip
    private lateinit var buttonSetReminder: Button
    private lateinit var dateTimeContainer: View
    private lateinit var backButton: ImageView
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var mapPickerLauncher: ActivityResultLauncher<Intent>

    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private val CHANNEL_ID = "ReminderChannel"
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedRadius: Double = 100.0 // Default radius in meters
    private var useLocationTrigger: Boolean = false
    private var selectedDays: MutableSet<Int> = mutableSetOf() // 1=Sunday, 2=Monday, ..., 7=Saturday
    private lateinit var radioGroupTriggerType: RadioGroup
    private lateinit var radioEnterLocation: RadioButton
    private lateinit var radioLeaveLocation: RadioButton
    private lateinit var radioAtLocation: RadioButton
    private lateinit var radioNotAtLocation: RadioButton
    private lateinit var radiusSlider: Slider
    private lateinit var radiusText: TextView
    private var selectedTriggerType: LocationTriggerType = LocationTriggerType.ENTER

    // Map preview variables
    private lateinit var buttonFullscreenMap: ImageButton
    private lateinit var buttonCurrentLocation: ImageButton
    private var previewMap: GoogleMap? = null
    private var mapPreviewFragment: SupportMapFragment? = null

    companion object {
        private const val TAG = "CreateReminderActivity"

        /**
         * Reusable helper to get current device location and update optional map/textView.
         * - context: Activity/Context used for permission checks & Toast.
         * - fusedLocationClient: an initialized FusedLocationProviderClient instance.
         * - googleMap: optional GoogleMap to place marker & move camera.
         * - textView: optional TextView to display lat,lng.
         * - onLocationSelected: callback with (lat, lng).
         */
        fun selectCurrentLocation(
            context: Context,
            fusedLocationClient: FusedLocationProviderClient,
            googleMap: GoogleMap? = null,
            textView: TextView? = null,
            onLocationSelected: ((Double, Double) -> Unit)? = null
        ) {
            val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val tokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            val lat = loc.latitude
                            val lng = loc.longitude

                            textView?.text = "$lat, $lng"

                            googleMap?.let {
                                it.clear()
                                val latLng = LatLng(lat, lng)
                                it.addMarker(MarkerOptions().position(latLng).title("Current Location"))
                                it.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                            }

                            onLocationSelected?.invoke(lat, lng)
                        } else {
                            // fallback to lastLocation
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { last ->
                                    if (last != null) {
                                        val lat = last.latitude
                                        val lng = last.longitude
                                        textView?.text = "$lat, $lng"
                                        googleMap?.let {
                                            it.clear()
                                            val latLng = LatLng(lat, lng)
                                            it.addMarker(MarkerOptions().position(latLng).title("Current Location"))
                                            it.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                        }
                                        onLocationSelected?.invoke(lat, lng)
                                    } else {
                                        Toast.makeText(context, "Unable to get location. Ensure GPS is on and try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "lastLocation failed: ${e.message}")
                                    Toast.makeText(context, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "getCurrentLocation failed: ${e.message}")
                        Toast.makeText(context, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting location: ${e.message}")
                Toast.makeText(context, "Error selecting location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_reminder)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // Initialize database
        val database = ReminderDatabase.getDatabase(this)
        reminderRepository = ReminderRepository(database.reminderDao())

        // Initialize location clients & geofencing
        geofencingClient = LocationServices.getGeofencingClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        initializeViews()
        setupLocationPermissionLauncher()
        setupMapPickerLauncher()
        setupClickListeners()
        prefillTodayDefaults()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder Notifications"
            val descriptionText = "Channel for reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun initializeViews() {
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextDate = findViewById(R.id.editTextDate)
        editTextTime = findViewById(R.id.editTextTime)
        editTextNotes = findViewById(R.id.editTextNotes)
        switchNotify = findViewById<SwitchMaterial>(R.id.switchNotify)

        // Initialize day chips
        chipMonday = findViewById(R.id.chipMonday)
        chipTuesday = findViewById(R.id.chipTuesday)
        chipWednesday = findViewById(R.id.chipWednesday)
        chipThursday = findViewById(R.id.chipThursday)
        chipFriday = findViewById(R.id.chipFriday)
        chipSaturday = findViewById(R.id.chipSaturday)
        chipSunday = findViewById(R.id.chipSunday)

        // Ensure chips are checkable at runtime (some Material versions may not accept app:checkable in XML)
        val chips = listOf(chipMonday, chipTuesday, chipWednesday, chipThursday, chipFriday, chipSaturday, chipSunday)
        chips.forEach { chip ->
            chip.isCheckable = true
            // Apply the ColorStateList selectors explicitly to ensure the checked state changes the visuals
            ContextCompat.getColorStateList(this, R.color.chip_bg_selector)?.let { csl ->
                chip.chipBackgroundColor = csl
            }
            ContextCompat.getColorStateList(this, R.color.chip_text_color_selector)?.let { tsl ->
                chip.setTextColor(tsl)
            }
        }

        // Initialize trigger type radio buttons
        radioGroupTriggerType = findViewById(R.id.radioGroupTriggerType)
        radioEnterLocation = findViewById(R.id.radioEnterLocation)
        radioLeaveLocation = findViewById(R.id.radioLeaveLocation)
        radioAtLocation = findViewById(R.id.radioAtLocation)
        radioNotAtLocation = findViewById(R.id.radioNotAtLocation)

        // Initialize map preview elements
        buttonFullscreenMap = findViewById(R.id.buttonFullscreenMap)
        buttonCurrentLocation = findViewById(R.id.buttonCurrentLocation)

        // Initialize radius slider
        radiusSlider = findViewById(R.id.radiusSlider)
        radiusText = findViewById(R.id.radiusText)

        // Initialize map preview fragment
        mapPreviewFragment = supportFragmentManager.findFragmentById(R.id.mapPreviewFragment) as? SupportMapFragment
        // Optional: location switch may or may not exist depending on layout changes
        findViewById<SwitchMaterial?>(R.id.switchLocation)?.let { locationSwitch ->
            locationSwitch.setOnCheckedChangeListener { _, isChecked ->
                useLocationTrigger = isChecked
                findViewById<View>(R.id.locationPickerContainer)?.visibility = if (isChecked) View.VISIBLE else View.GONE
                // Initialize map when location trigger is enabled
                if (isChecked) {
                    initializeMapPreview()
                }
            }
        }
        buttonSetReminder = findViewById(R.id.buttonSetReminder)
        backButton = findViewById(R.id.backButton)
        dateTimeContainer = findViewById(R.id.dateTimeContainer)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        editTextDate.setOnClickListener {
            showDatePicker()
        }

        editTextTime.setOnClickListener {
            showTimePicker()
        }

        // Toggle date/time visibility when notifications switch changes
        switchNotify.setOnCheckedChangeListener { _, isChecked ->
            dateTimeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        buttonSetReminder.setOnClickListener {
            saveReminder()
        }

        // Setup day chip listeners
        setupDayChipListeners()

        // Setup trigger type radio button listener
        radioGroupTriggerType.setOnCheckedChangeListener { _, checkedId ->
            selectedTriggerType = when (checkedId) {
                R.id.radioEnterLocation -> LocationTriggerType.ENTER
                R.id.radioLeaveLocation -> LocationTriggerType.LEAVE
                R.id.radioAtLocation -> LocationTriggerType.AT
                R.id.radioNotAtLocation -> LocationTriggerType.NOT_AT
                else -> LocationTriggerType.ENTER
            }
            Log.d(TAG, "Selected trigger type: $selectedTriggerType")
        }

        // Setup map preview button listeners
        buttonFullscreenMap.setOnClickListener {
            // Launch fullscreen map picker
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        buttonCurrentLocation.setOnClickListener {
            // Check permissions and then call companion helper
            ensureLocationPermissionThenSelect()
        }

        // Setup radius slider listener
        radiusSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                selectedRadius = value.toDouble()
                radiusText.text = "${value.toInt()} meters"
                // Update map preview if location is selected
                if (selectedLatitude != null && selectedLongitude != null) {
                    updateMapPreview(selectedLatitude!!, selectedLongitude!!)
                }
            }
        }
    }

    private fun setupDayChipListeners() {
        chipMonday.setOnClickListener {
            toggleDaySelection(chipMonday, Calendar.MONDAY)
        }
        chipTuesday.setOnClickListener {
            toggleDaySelection(chipTuesday, Calendar.TUESDAY)
        }
        chipWednesday.setOnClickListener {
            toggleDaySelection(chipWednesday, Calendar.WEDNESDAY)
        }
        chipThursday.setOnClickListener {
            toggleDaySelection(chipThursday, Calendar.THURSDAY)
        }
        chipFriday.setOnClickListener {
            toggleDaySelection(chipFriday, Calendar.FRIDAY)
        }
        chipSaturday.setOnClickListener {
            toggleDaySelection(chipSaturday, Calendar.SATURDAY)
        }
        chipSunday.setOnClickListener {
            toggleDaySelection(chipSunday, Calendar.SUNDAY)
        }
    }

    private fun toggleDaySelection(chip: Chip, dayOfWeek: Int) {
        // Toggle checked state
        chip.isChecked = !chip.isChecked

        // Maintain selectedDays set
        if (chip.isChecked) {
            selectedDays.add(dayOfWeek)
        } else {
            selectedDays.remove(dayOfWeek)
        }

        // Force visual update for chips that may not pick up ColorStateList reliably
        updateChipVisual(chip, chip.isChecked)
    }

    private fun updateChipVisual(chip: Chip, checked: Boolean) {
        try {
            val bgDrawable = if (checked) {
                ContextCompat.getDrawable(this, R.drawable.chip_bg_checked)
            } else {
                ContextCompat.getDrawable(this, R.drawable.chip_bg_unchecked)
            }

            // Apply drawable directly to the Chip's background to override theme styles
            chip.background = bgDrawable

            // Update text color explicitly
            val textColor = if (checked) R.color.colorOnPrimary else R.color.colorOnSurface
            chip.setTextColor(ContextCompat.getColor(this, textColor))

            // Force a layout refresh
            chip.invalidate()
            chip.requestLayout()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update chip visual: ${e.message}")
        }
    }

    private fun initializeMapPreview() {
        try {
            val availability = GoogleApiAvailability.getInstance()
            val status = availability.isGooglePlayServicesAvailable(this)
            if (status != ConnectionResult.SUCCESS) {
                val errorString = availability.getErrorString(status)
                Toast.makeText(this, "Google Play Services unavailable: $errorString", Toast.LENGTH_LONG).show()
                return
            }

            if (mapPreviewFragment == null) {
                mapPreviewFragment = supportFragmentManager.findFragmentById(R.id.mapPreviewFragment) as? SupportMapFragment
            }
            mapPreviewFragment?.getMapAsync(this)
        } catch (e: Exception) {
            Log.e(TAG, "initializeMapPreview error: ${e.message}")
            Toast.makeText(this, "Map initialization error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        previewMap = map

        try {
            // Set up the map
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false

            // Set initial camera position
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        Log.d(TAG, "Initial location set to current location: $currentLatLng")
                    } else {
                        Log.w(TAG, "Current location is null, falling back to default")
                        val fallback = LatLng(37.4219999, -122.0840575) // fallback
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 15f))
                    }
                }
            }


            // If we have a selected location, show it
            selectedLatitude?.let { lat ->
                selectedLongitude?.let { lng ->
                    val location = LatLng(lat, lng)
                    map.addMarker(MarkerOptions().position(location))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up map preview: ${e.message}")
        }
    }

    private fun updateMapPreview(latitude: Double, longitude: Double) {
        previewMap?.let { map ->
            try {
                map.clear() // Clear existing markers and circles
                val location = LatLng(latitude, longitude)
                map.addMarker(MarkerOptions().position(location))
                
                // Add radius circle
                map.addCircle(CircleOptions()
                    .center(location)
                    .radius(selectedRadius)
                    .strokeWidth(2f)
                    .strokeColor(0x552196F3)
                    .fillColor(0x332196F3))
                
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                Log.d(TAG, "Map preview updated with location: $latitude, $longitude, radius: $selectedRadius")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating map preview: ${e.message}")
            }
        }
    }

    private fun setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                // call companion helper
                CreateReminderActivity.selectCurrentLocation(
                    this,
                    fusedLocationClient,
                    previewMap,
                    findViewById(R.id.textSelectedLocation)
                ) { lat, lng ->
                    selectedLatitude = lat
                    selectedLongitude = lng
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMapPickerLauncher() {
        mapPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedLatitude = result.data!!.getDoubleExtra("lat", 0.0)
                selectedLongitude = result.data!!.getDoubleExtra("lng", 0.0)
                selectedRadius = result.data!!.getDoubleExtra("radius", 100.0)
                findViewById<TextView?>(R.id.textSelectedLocation)?.text = "${selectedLatitude}, ${selectedLongitude} (${selectedRadius.toInt()}m)"
                updateMapPreview(selectedLatitude!!, selectedLongitude!!)
            }
        }
    }

    private fun ensureLocationPermissionThenSelect() {
        val hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            // call companion helper
            CreateReminderActivity.selectCurrentLocation(
                this,
                fusedLocationClient,
                previewMap,
                findViewById(R.id.textSelectedLocation)
            ) { lat, lng ->
                selectedLatitude = lat
                selectedLongitude = lng
            }
        }
    }

    private fun addGeofenceForReminder(title: String) {
        val latitude = selectedLatitude
        val longitude = selectedLongitude
        if (latitude == null || longitude == null) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            return
        }

        // Determine transition types based on selected trigger type
        val transitionTypes = when (selectedTriggerType) {
            LocationTriggerType.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
            LocationTriggerType.LEAVE -> Geofence.GEOFENCE_TRANSITION_EXIT
            LocationTriggerType.AT -> Geofence.GEOFENCE_TRANSITION_DWELL
            LocationTriggerType.NOT_AT -> Geofence.GEOFENCE_TRANSITION_EXIT
        }

        val geofence = Geofence.Builder()
            .setRequestId("reminder_${System.currentTimeMillis()}")
            .setCircularRegion(latitude, longitude, selectedRadius.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .apply {
                // Set dwell time for "at location" trigger
                if (selectedTriggerType == LocationTriggerType.AT) {
                    setLoiteringDelay(10000) // 10 seconds dwell time
                }
            }
            .build()

        val initialTrigger = when (selectedTriggerType) {
            LocationTriggerType.ENTER -> GeofencingRequest.INITIAL_TRIGGER_ENTER
            LocationTriggerType.LEAVE -> GeofencingRequest.INITIAL_TRIGGER_EXIT
            LocationTriggerType.AT -> GeofencingRequest.INITIAL_TRIGGER_DWELL
            LocationTriggerType.NOT_AT -> GeofencingRequest.INITIAL_TRIGGER_EXIT
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(initialTrigger)
            .addGeofence(geofence)
            .build()

        val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("trigger_type", selectedTriggerType.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            Toast.makeText(this, "Location permission required for geofencing", Toast.LENGTH_SHORT).show()
            return
        }

        // Request background location on Android 10+ when using geofencing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground) {
                try {
                    locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                    Toast.makeText(this, "Background location required for geofencing", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to request background location: ${e.message}")
                }
                return
            }
        }

        try {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added successfully with trigger type: $selectedTriggerType")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add geofence: ${e.message}")
                    Toast.makeText(this, "Failed to add geofence: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (se: SecurityException) {
            Log.e(TAG, "Security exception adding geofence: ${se.message}")
            Toast.makeText(this, "Location permission missing for geofencing", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error adding geofence: ${e.message}")
            Toast.makeText(this, "Error adding geofence: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ... rest of your functions (date/time pickers, scheduleAlarm, saveReminder, formatDateTimeForDisplay, etc.)
    // For brevity, they are kept as-is from your original file (no logic change).
    // I assume you will keep the saveReminder(), scheduleAlarm(), prefillTodayDefaults(), showDatePicker(), showTimePicker(), formatDateTimeForDisplay() implementations from your original file.


private fun prefillTodayDefaults() {
        // Default date to today and time to next hour
        val now = Calendar.getInstance()
        selectedDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
        }
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        editTextDate.setText(dateFormat.format(selectedDate!!.time))

        selectedTime = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        editTextTime.setText(timeFormat.format(selectedTime!!.time))

        // Start with date/time hidden until notifications are enabled
        dateTimeContainer.visibility = if (switchNotify.isChecked) View.VISIBLE else View.GONE
        findViewById<View>(R.id.locationPickerContainer)?.visibility = if (useLocationTrigger) View.VISIBLE else View.GONE
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val customDatePicker = CustomDatePickerDialog.newInstance(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        customDatePicker.setOnDateSetListener(object : CustomDatePickerDialog.OnDateSetListener {
            override fun onDateSet(year: Int, month: Int, dayOfMonth: Int) {
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                editTextDate.setText(dateFormat.format(selectedDate!!.time))
            }
        })
        
        customDatePicker.show(supportFragmentManager, "CustomDatePicker")
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val customTimePicker = CustomTimePickerDialog.newInstance(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // Use 12-hour format
        )
        
        customTimePicker.setOnTimeSetListener(object : CustomTimePickerDialog.OnTimeSetListener {
            override fun onTimeSet(hourOfDay: Int, minute: Int) {
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                editTextTime.setText(timeFormat.format(selectedTime!!.time))
            }
        })
        
        customTimePicker.show(supportFragmentManager, "CustomTimePicker")
    }

    private fun saveReminder() {
        val title = editTextTitle.text.toString().trim()
        val notes = editTextNotes.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (switchNotify.isChecked) {
            if (selectedDate == null) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedTime == null) {
                Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {
            // Format the date and time for display
            val displayDateTime = if (switchNotify.isChecked) formatDateTimeForDisplay() else "No notification"

            // Create reminder object
            val reminder = ReminderEntity(
                title = title,
                dateTime = displayDateTime,
                iconResId = R.drawable.ic_bell,
                status = ReminderStatus.PENDING,
                repeatDays = selectedDays.sorted().joinToString(","),
                isCompleted = false,
                notes = notes
            )

            // Save to database
            lifecycleScope.launch {
                try {
                    reminderRepository.insert(reminder)

                    if (switchNotify.isChecked) {
                        val scheduledTime = scheduleAlarm(title, notes)
                        Log.d(TAG, "Reminder scheduled for: $scheduledTime")
                    } else {
                        Log.d(TAG, "Notify disabled; no alarm scheduled")
                    }

                    if (useLocationTrigger && selectedLatitude != null && selectedLongitude != null) {
                        addGeofenceForReminder(title)
                    }

                    val message = when {
                        useLocationTrigger -> {
                            val triggerText = when (selectedTriggerType) {
                                LocationTriggerType.ENTER -> "when entering location"
                                LocationTriggerType.LEAVE -> "when leaving location"
                                LocationTriggerType.AT -> "while at location"
                                LocationTriggerType.NOT_AT -> "while not at location"
                            }
                            "Reminder set: $title $triggerText"
                        }
                        switchNotify.isChecked -> "Reminder set: $title at $displayDateTime"
                        else -> "Reminder saved without notification"
                    }
                    Toast.makeText(this@CreateReminderActivity, message, Toast.LENGTH_LONG).show()

                    // Return to main activity
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving reminder to database: ${e.message}")
                    Toast.makeText(this@CreateReminderActivity, "Error saving reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reminder: ${e.message}")
            Toast.makeText(this, "Error setting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDateTimeForDisplay(): String {
        val now = Calendar.getInstance()
        val reminderDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(selectedTime!!.time)

        val baseDateString = when {
            // Today
            reminderDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            reminderDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> {
                "Today, $timeString"
            }
            // Tomorrow
            reminderDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            reminderDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) + 1 -> {
                "Tomorrow, $timeString"
            }
            // This week
            reminderDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            reminderDate.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                "${dayFormat.format(reminderDate.time)}, $timeString"
            }
            // Other dates
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${dateFormat.format(reminderDate.time)}, $timeString"
            }
        }

        // Add repeat information if days are selected
        return if (selectedDays.isNotEmpty()) {
            val dayNames = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val selectedDayNames = selectedDays.sorted().map { dayNames[it] }
            "$baseDateString (Repeats: ${selectedDayNames.joinToString(", ")})"
        } else {
            baseDateString
        }
    }

    private fun scheduleAlarm(title: String, notes: String): String {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReciver::class.java).apply {
            putExtra("title", title)
            putExtra("notes", notes)
        }

        // Generate unique ID for each reminder
        val uniqueId = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Combine date and time properly
        val reminderTime = Calendar.getInstance().apply {
            // Set the date
            set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))

            // Set the time
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time has already passed today, schedule for tomorrow
        if (reminderTime.timeInMillis <= System.currentTimeMillis()) {
            reminderTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val scheduledTimeString = timeFormat.format(reminderTime.time)

        Log.d(TAG, "Scheduling alarm for: $scheduledTimeString")

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Alarm scheduled successfully with exact timing")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not allowed, using regular alarm")
            // Handle case where exact alarms are not allowed
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
            throw e
        }

        return scheduledTimeString
    }
}

enum class LocationTriggerType {
    ENTER,      // Entering location
    LEAVE,      // Leaving location
    AT,         // While at location
    NOT_AT      // While not at location
}