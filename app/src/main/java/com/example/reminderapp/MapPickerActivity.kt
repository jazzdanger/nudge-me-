package com.example.reminderapp

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource



private lateinit var fusedLocationClient: FusedLocationProviderClient


class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private var circle: Circle? = null
    private var center: LatLng? = null
    private var radiusMeters: Double = 100.0
    
    companion object {
        private const val TAG = "MapPickerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<android.view.View>(R.id.buttonMyLocation).setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        drawCircle(currentLatLng, radiusMeters)
                        center = currentLatLng
                        Log.d(TAG, "Moved to current location: $currentLatLng")
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            }
        }



        Log.d(TAG, "MapPickerActivity onCreate started")
        
        // Test Google Maps API key
        testGoogleMapsApiKey()
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, getting map async")
            mapFragment.getMapAsync(this)
        } else {
            Log.e(TAG, "Map fragment not found!")
            Toast.makeText(this, "Error: Map fragment not found", Toast.LENGTH_LONG).show()
        }

        findViewById<android.view.View>(R.id.buttonConfirmLocation).setOnClickListener {
            center?.let { c ->
                val result = Intent().apply {
                    putExtra("lat", c.latitude)
                    putExtra("lng", c.longitude)
                }
                setResult(RESULT_OK, result)
                finish()
            } ?: run {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun testGoogleMapsApiKey() {
        val apiKey = getString(R.string.google_maps_key)
        Log.d(TAG, "Google Maps API Key: ${apiKey.take(10)}...")
        
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
            Log.e(TAG, "Invalid or missing Google Maps API key!")
            Toast.makeText(this, "Error: Invalid Google Maps API key", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "Google Maps API key appears to be valid")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d(TAG, "Map is ready!")
        googleMap = map
        
        try {
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
                        drawCircle(currentLatLng, radiusMeters)
                        center = currentLatLng
                        Log.d(TAG, "Initial location set to current location: $currentLatLng")
                    } else {
                        Log.w(TAG, "Current location is null, falling back to default")
                        val fallback = LatLng(37.4219999, -122.0840575) // fallback
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 15f))
                    }
                }
            }


            map.setOnMapClickListener { latLng ->
                Log.d(TAG, "Map clicked at: ${latLng.latitude}, ${latLng.longitude}")
                center = latLng
                drawCircle(latLng, radiusMeters)
                Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show()
            }
            
            // Enable user location if permission is granted
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
                Log.d(TAG, "Location enabled on map")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up map: ${e.message}")
            Toast.makeText(this, "Error setting up map: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawCircle(latLng: LatLng, radius: Double) {
        try {
            circle?.remove()
            circle = googleMap?.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(radius)
                    .strokeWidth(2f)
                    .strokeColor(0x552196F3)
                    .fillColor(0x332196F3)
            )
            Log.d(TAG, "Circle drawn at: ${latLng.latitude}, ${latLng.longitude}")
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing circle: ${e.message}")
        }
    }
}


