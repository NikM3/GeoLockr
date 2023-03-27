package com.nikm3.geolockr

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.nikm3.geolockr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val runningTiramisuOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentLockButton = binding.currentLocationButton
        val destinationLockButton = binding.unstilDestinationButton
        val drivingLockButton = binding.drivingButton
        val whereAmIButton = binding.whereButton

        currentLockButton.setOnClickListener {
            if (checkPermissions()) {
                val intent = Intent(this, LockActivity::class.java)
                intent.putExtra(EXTRA_MODE, 1)
                startActivity(intent)
            }
        }

        destinationLockButton.setOnClickListener {
            if (checkPermissions()) {
                val intent = Intent(this, LockActivity::class.java)
                intent.putExtra(EXTRA_MODE, 2)
                startActivity(intent)
            }

        }

        /**
         * Note: Currently non-functional
         */
        drivingLockButton.setOnClickListener {
            val intent = Intent(this, LockActivity::class.java)
            intent.putExtra(EXTRA_MODE, 3)
            startActivity(intent)
            finish()

        }

        whereAmIButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    /**
     * Call other methods to check if permissions have been granted
     * If not, ask for them
     */
    private fun checkPermissions() : Boolean {
        return if (permissionsApproved()) {
            checkDeviceLocationSettings()
            true
        } else {
            requestNecessaryPermissions()
            false
        }
    }

    /**
     * Ensure the User has Location Services enabled, prompt them if not
     */
    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        this@MainActivity,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    findViewById(R.id.activity_main),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                /* Do something */
            }
        }
    }

    /**
     * Check if permissions are enabled
     */
    @SuppressLint("InlinedApi")
    @TargetApi(29)
    private fun permissionsApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        val notificationPermissionApproved =
            if (runningTiramisuOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }
        return foregroundLocationApproved
                && backgroundPermissionApproved
                && notificationPermissionApproved
    }


    /**
     *  Requests necessary permissions
     *  All versions: ACCESS_FINE_LOCATION
     *  Android 10+: ACCESS_BACKGROUND_LOCATION
     *  Android 33+: POST_NOTIFICATIONS
     */
    @TargetApi(33 )
    private fun requestNecessaryPermissions() {
        if (permissionsApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            runningTiramisuOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                permissionsArray += Manifest.permission.POST_NOTIFICATIONS
                REQUEST_ALL_PERMISSIONS
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            this@MainActivity,
            permissionsArray,
            resultCode
        )
    }

    /**
     * Inform the User that permissions are required if not granted
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            /* Do something since Permissions are enabled */
        }
    }


    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "MainActivity.ACTION_GEOFENCE_EVENT"
        const val EXTRA_MODE = "lock_screen_mode"
    }
}

private const val REQUEST_ALL_PERMISSIONS = 32
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "MainActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1