package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.DialogInterface
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.BatteryManager
import com.google.android.gms.maps.model.LatLng

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
    private lateinit var dangerAlertView: View
    private lateinit var dangerAlert: AlertDialog
    private lateinit var logTextView: TextView
    private var lastKnownLocation: Location? = null

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

        // Check and request the READ_PHONE_STATE permission if not granted
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

        // Check and request the ACCESS_FINE_LOCATION permission if not granted
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
        logTextView = findViewById(R.id.logTextView)

        // Set the initial message as "Not Checking Network Status"
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

        // Inflate the custom danger alert layout
        dangerAlertView = layoutInflater.inflate(R.layout.danger_alert_layout, null)
        dangerAlert = AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setView(dangerAlertView)
            .setPositiveButton("OK", null)
            .create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dangerAlert.dismiss() // Dismiss the danger alert dialog when the activity is destroyed
        locationManager.removeUpdates(locationListener) // Stop receiving location updates
    }

    private fun startCheckingNetworkStatus() {
        if (isCheckingNetworkStatus) {
            return
        }

        isCheckingNetworkStatus = true
        statusTextView.apply {
            text = "Checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorCheckingStatus))
            visibility = View.VISIBLE
        }
        handler.postDelayed(networkCheckRunnable, networkCheckIntervalMs)
    }

    private fun stopCheckingNetworkStatus() {
        isCheckingNetworkStatus = false
        handler.removeCallbacks(networkCheckRunnable)
        statusTextView.apply {
            text = "Not checking network status"
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotCheckingStatus))
            visibility = View.VISIBLE
        }
    }

    private val networkCheckRunnable = object : Runnable {
        override fun run() {
            if (!isCheckingNetworkStatus) {
                return
            }

            logNetworkType()
            handler.postDelayed(this, networkCheckIntervalMs)
        }
    }

    private fun logNetworkType() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = networkCapabilities != null

        if (isConnected) {
            when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                    val networkType = telephonyManager.networkType
                    val mobileNetworkType = getMobileNetworkType(networkType)
                    val signalStrength = getSignalStrength()
                    val batteryStatus = getBatteryStatus()
                    Log.d(
                        "Network",
                        "Connected to Mobile Data: $mobileNetworkType, Signal Strength: $signalStrength, Battery Status: $batteryStatus"
                    )

                    if ((mobileNetworkType == "2G" || mobileNetworkType == "3G") &&
                        (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) &&
                        !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) &&
                        !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    ) {

                        val currentDateTime = getCurrentDateTime()
                        val location = getLastKnownLocation()
                        val distanceFromLastKnownLocation = calculateDistanceFromLastKnownLocation(location)

                        if (distanceFromLastKnownLocation >= 100000) {
                            val logMessage =
                                "$currentDateTime - Warning: Network protocol downgraded from 4G/5G to $mobileNetworkType. Location might be under a spoof attack! Distance from last known location: $distanceFromLastKnownLocation meters"
                            appendToLog(logMessage)
                        }
                    }
                }
                else -> {
                    Log.d("Network", "Connected to Other Network")
                }
            }
        } else {
            Log.d("Network", "Not Connected")
        }
    }


    private fun calculateDistanceFromLastKnownLocation(currentLocation: Location?): Float {
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) }
        val lastKnownLatLng = lastKnownLocation?.let { LatLng(it.latitude, it.longitude) }

        if (currentLatLng != null && lastKnownLatLng != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                lastKnownLatLng.latitude, lastKnownLatLng.longitude,
                currentLatLng.latitude, currentLatLng.longitude,
                results
            )
            return results[0]
        }
        return 0f
    }

    private fun extractLatLng(locationString: String): LatLng? {
        val pattern = "Lat: (.*), Long: (.*)".toRegex()
        val matchResult = pattern.find(locationString)
        if (matchResult != null) {
            val (latitude, longitude) = matchResult.destructured
            return LatLng(latitude.toDouble(), longitude.toDouble())
        }
        return null
    }

    private fun getBatteryStatus(): String {
        val batteryStatus = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPercentage = batteryStatus.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "$batteryPercentage%"
    }

    private fun getSignalStrength(): String {
        val signalStrength = telephonyManager.signalStrength
        return if (signalStrength != null) {
            when (signalStrength.level) {
                0 -> "None"
                1 -> "Poor"
                2 -> "Moderate"
                3 -> "Good"
                else -> "Excellent"
            }
        } else {
            "Unknown"
        }
    }

    private fun getLastKnownLocation(): Location? {
        var location: Location? = null
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        return location
    }

    private fun initializeLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Do nothing here; we only need the last known location
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Do nothing here
            }

            override fun onProviderEnabled(provider: String) {
                // Do nothing here
            }

            override fun onProviderDisabled(provider: String) {
                // Do nothing here
            }
        }

        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null)
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateTime = Date()
        return dateFormat.format(currentDateTime)
    }

    private fun appendToLog(message: String) {
        val logTextView: TextView = findViewById(R.id.logTextView)
        logTextView.append("$message")
    }

    private fun getMobileNetworkType(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"

            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN,
            TelephonyManager.NETWORK_TYPE_NR -> "4G/5G"

            else -> "Unknown"
        }
    }

    private fun showDangerAlert() {
        // Displaying the warning alert is not necessary since we are appending the message to the log
    }

    private fun hideDangerAlert() {
        // Hiding the warning alert is not necessary since we are appending the message to the log
    }

}