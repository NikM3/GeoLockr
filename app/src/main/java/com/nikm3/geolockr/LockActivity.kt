package com.nikm3.geolockr

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.nikm3.geolockr.databinding.ActivityLockBinding

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofence: Geofence
    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = MainActivity.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val message = binding.textView
        geofencingClient = LocationServices.getGeofencingClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel(this)

        when (intent.getStringExtra(MainActivity.EXTRA_MODE)!!.toInt()) {
            1 -> { // User selected current location lock
                geofenceCurrentLocation()
                message.text = getString(R.string.current_lock_message, GEOFENCE_RADIUS_IN_METERS)
            }
            2 -> { // User selected destination lock
                geofenceDestination()
                message.text = getString(R.string.destination_lock_message)
            }
            else -> {
                // Entered on inappropriate mode, go back to MainActivity
                Toast.makeText(this, "Something has gone horribly wrong", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Add Geofence around current location, activate when left
     */
    @SuppressLint("MissingPermission")
    private fun geofenceCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                geofence = Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(CURRENT)

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build()
            }
        }
    }

    /**
     * Add Geofence around destination, activate when entered
     */
    @SuppressLint("MissingPermission")
    private fun geofenceDestination() {
            geofence = Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(CURRENT)

                // Set the circular region of this geofence.
                .setCircularRegion(
                    MOCK_LAT_LNG.latitude,
                    MOCK_LAT_LNG.longitude,
                    GEOFENCE_RADIUS_IN_METERS
                )

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build()
    }

    companion object {
        const val CURRENT = "current_location"
        const val DESTINATION = "destination_location"
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        val MOCK_LAT_LNG = LatLng(50.0, 50.0)
    }
}