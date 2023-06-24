package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.gms.maps.model.LatLng
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private val handler = Handler()
    private val networkCheckIntervalMs = 10000L
    private var isCheckingNetworkStatus = false
    private lateinit var statusTextView: TextView
    private var lastKnownLocation: Location? = null
    private var lastKnownNetworkType: String? = null
    private lateinit var networkProtocolTextView: TextView


    // Initialize the network callback for monitoring network availability
    private fun initializeNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                logNetworkType()
            }

            override fun onLost(network: Network) {
                logNetworkType()
            }
        }

        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    companion object {
        private const val PERMISSION_REQUEST_READ_PHONE_STATE = 1
        private const val PERMISSION_REQUEST_LOCATION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        networkProtocolTextView = findViewById(R.id.networkProtocolTextView)
        networkProtocolTextView.visibility = View.VISIBLE

        // Request READ_PHONE_STATE permission if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PERMISSION_REQUEST_READ_PHONE_STATE
            )
        } else {
            initializeNetworkCallback()
        }

        // Request ACCESS_FINE_LOCATION permission if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        } else {
            initializeLocationListener()
        }

        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)

        statusTextView.apply {
            text = "Not checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotCheckingStatus))
            visibility = View.VISIBLE
        }

        startButton.setOnClickListener {
            startCheckingNetworkStatus()
        }

        stopButton.setOnClickListener {
            stopCheckingNetworkStatus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }

    // Start checking the network status
    private fun startCheckingNetworkStatus() {
        if (isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = true
        statusTextView.apply {
            text = "Checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCheckingStatus))
        }

        handler.post(networkCheckRunnable)
    }

    // Stop checking the network status
    private fun stopCheckingNetworkStatus() {
        if (!isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = false
        statusTextView.apply {
            text = "Not checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotCheckingStatus))
        }

        handler.removeCallbacks(networkCheckRunnable)
    }

    // Log the current network type
    private fun logNetworkType() {
        val networkType = telephonyManager.networkType
        val currentNetworkType = getMobileNetworkType(networkType)
        lastKnownNetworkType = currentNetworkType
        networkProtocolTextView.text = "Current Network Protocol: $currentNetworkType"
    }

    // Runnable for network status checks
    private val networkCheckRunnable = object : Runnable {
        override fun run() {
            if (!isCheckingNetworkStatus) {
                return
            }

            val previousNetworkType = lastKnownNetworkType

            logNetworkType()
            val currentNetworkType = lastKnownNetworkType

            val currentDateTime = getCurrentDateTime()
            var logMessage = ""

            // Check for network protocol downgrade
            if (previousNetworkType in listOf("4G", "5G") && currentNetworkType in listOf("2G", "3G")) {
                logMessage =
                    "$currentDateTime - Warning: Network protocol downgraded from $previousNetworkType to $currentNetworkType. Possible network interference detected!"
            }

            if (logMessage.isNotEmpty()) {
                appendToLog(logMessage)
            }

            handler.postDelayed(this, networkCheckIntervalMs)
        }
    }

    // Get the mobile network type based on network type constant
    private fun getMobileNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GSM -> "2G"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            TelephonyManager.NETWORK_TYPE_CDMA -> "2G"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "3G"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "3G"
            TelephonyManager.NETWORK_TYPE_HSPA -> "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "3G"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }

    // Get the current date and time in a specific format
    private fun getCurrentDateTime(): String {
        val currentDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
        return currentDateTime
    }

    // Append a log message to the status text view and logcat
    private fun appendToLog(logMessage: String) {
        Log.d("MainActivity", logMessage)
        runOnUiThread {
            statusTextView.append("\n$logMessage")
        }
    }

    // Get the last known location from the location manager
    private fun getLastKnownLocation(): Location? {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            null
        } else {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
    }

    // Calculate the distance between the previous and current locations
    private fun calculateDistanceFromLastKnownLocation(currentLocation: Location?): Float {
        return if (lastKnownLocation != null && currentLocation != null) {
            val previousLatLng = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            val results = FloatArray(1)
            Location.distanceBetween(
                previousLatLng.latitude,
                previousLatLng.longitude,
                currentLatLng.latitude,
                currentLatLng.longitude,
                results
            )
            results[0]
        } else {
            Float.MAX_VALUE
        }
    }

    // Initialize the location listener for monitoring location changes
    private fun initializeLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val currentDistance = calculateDistanceFromLastKnownLocation(location)

                if (currentDistance > 30) {
                    lastKnownLocation = location

                    val currentNetworkType = lastKnownNetworkType
                    val currentDateTime = getCurrentDateTime()

                    // Check for significant location change without network protocol downgrade
                    if (currentNetworkType in listOf("3G", "2G")) {
                        val logMessage =
                            "$currentDateTime - Warning: Network protocol downgraded from 5G/4G to $currentNetworkType with a change in location. Possible mobile device tracking detected!"
                        appendToLog(logMessage)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_PHONE_STATE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initializeNetworkCallback()
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("The app needs the READ_PHONE_STATE permission to function properly. Please grant it in your device settings.")
                        .setPositiveButton("OK") { _, _ -> }
                        .create()
                        .show()
                }
            }
            PERMISSION_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initializeLocationListener()
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("The app needs the ACCESS_FINE_LOCATION permission to function properly. Please grant it in your device settings.")
                        .setPositiveButton("OK") { _, _ -> }
                        .create()
                        .show()
                }
            }
        }
    }
}
