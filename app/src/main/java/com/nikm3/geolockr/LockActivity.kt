package com.nikm3.geolockr

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.nikm3.geolockr.databinding.ActivityLockBinding

@RequiresApi(Build.VERSION_CODES.S)
class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofence: Geofence
    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = MainActivity.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val message = binding.textView
        val button = binding.unlockButton
        geofencingClient = LocationServices.getGeofencingClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencePendingIntent.isImmutable
        createChannel(this)

        when (intent.getIntExtra(MainActivity.EXTRA_MODE,0)) {
            1 -> { // User selected current location lock
                Log.d(TAG,"Made it into Current Location Switch Case")
                geofenceCurrentLocation()
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    message.text =
                        getString(R.string.current_lock_message,
                            GEOFENCE_RADIUS_IN_METERS,
                            location!!.latitude.toString(),
                            location.longitude.toString())
                }
            }
            2 -> { // User selected destination lock
                Log.d(TAG,"Made it into Destination Switch Case")
                geofenceDestination()
                message.text = getString(R.string.destination_lock_message,
                        MOCK_LAT_LNG.latitude.toString(),
                        MOCK_LAT_LNG.longitude.toString())
            }
            else -> {
                // Entered on inappropriate mode, go back to MainActivity
                Toast.makeText(this, "Something has gone horribly wrong", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Cleanup the app and remove any Geofences
     */
    override fun onDestroy() {
        super.onDestroy()
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    /**
     * Add Geofence around current location, activate when left
     */
    @SuppressLint("MissingPermission", "VisibleForTests")
    private fun geofenceCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                geofence = Geofence.Builder()
                    .setRequestId(CURRENT)
                    .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        GEOFENCE_RADIUS_IN_METERS
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
                Log.d(TAG,"Built GeoFence")

                val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    .addGeofence(geofence)
                    .build()
                Log.d(TAG,"Built Request")

                geofencingClient.removeGeofences(geofencePendingIntent).run {
                    addOnCompleteListener {
                        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                            addOnSuccessListener {
                                Toast.makeText(
                                    this@LockActivity, R.string.geofence_added,
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                Log.d("Add Geofence", geofence.requestId)
                            }
                            addOnFailureListener {
                                Toast.makeText(
                                    this@LockActivity, R.string.geofences_not_added,
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (it.message != null) {
                                    Log.w(TAG, it.message!!)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Add Geofence around destination, activate when entered
     */
    @SuppressLint("MissingPermission", "VisibleForTests")
    private fun geofenceDestination() {
        geofence = Geofence.Builder()
            .setRequestId(DESTINATION)
            .setCircularRegion(
                MOCK_LAT_LNG.latitude,
                MOCK_LAT_LNG.longitude,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        Log.d(TAG,"Built GeoFence")

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        Log.d(TAG,"Built Request")

        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Toast.makeText(
                            this@LockActivity, R.string.geofence_added,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        Log.d("Add Geofence", geofence.requestId)
                    }
                    addOnFailureListener {
                        Toast.makeText(
                            this@LockActivity, R.string.geofences_not_added,
                            Toast.LENGTH_SHORT
                        ).show()
                        if (it.message != null) {
                            Log.w(TAG, it.message!!)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val CURRENT = "current_location"
        const val DESTINATION = "destination_location"
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        val MOCK_LAT_LNG = LatLng(33.9965,-81.0271)
        const val TAG = "LockActivity"
    }
}